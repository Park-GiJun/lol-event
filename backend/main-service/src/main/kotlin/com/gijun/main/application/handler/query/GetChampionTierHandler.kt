package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionTierEntry
import com.gijun.main.application.dto.stats.result.ChampionTierResult
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetChampionTierHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
) : GetChampionTierUseCase {

    override fun getChampionTier(mode: String, minGames: Int): ChampionTierResult = cache.getOrCompute("champion-tier:$mode:$minGames") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        fun r2(v: Double) = (v * 100).toInt() / 100.0

        val totalMatches = matches.size
        if (totalMatches == 0) return@getOrCompute ChampionTierResult(emptyList(), emptyMap(), 0)

        data class ChampionAcc(
            val champion: String,
            var championId: Int = 0,
            var games: Int = 0,
            var wins: Int = 0,
            var kills: Int = 0,
            var deaths: Int = 0,
            var assists: Int = 0,
            var totalDamage: Long = 0L,
        )

        val champMap = mutableMapOf<String, ChampionAcc>()

        for (m in matches) {
            for (p in m.participants) {
                val acc = champMap.getOrPut(p.champion) { ChampionAcc(p.champion) }
                acc.championId = p.championId
                acc.games++
                if (p.win) acc.wins++
                acc.kills += p.kills
                acc.deaths += p.deaths
                acc.assists += p.assists
                acc.totalDamage += p.damage
            }
        }

        // 전체 평균 KDA, 전체 평균 데미지
        val allParticipants = matches.flatMap { it.participants }
        val overallAvgKda = run {
            val k = allParticipants.sumOf { it.kills }
            val d = allParticipants.sumOf { it.deaths }
            val a = allParticipants.sumOf { it.assists }
            if (d > 0) (k + a).toDouble() / d else (k + a).toDouble()
        }
        val overallAvgDamage = if (allParticipants.isNotEmpty())
            allParticipants.sumOf { it.damage }.toDouble() / allParticipants.size
        else 1.0

        // 티어 점수 계산 (minGames 미만 포함 모두 계산, 티어만 "?"로)
        val scored = champMap.values.map { acc ->
            val g = acc.games.coerceAtLeast(1)
            val winRate = acc.wins.toDouble() / g
            val avgKda = if (acc.deaths > 0) (acc.kills + acc.assists).toDouble() / acc.deaths
                         else (acc.kills + acc.assists).toDouble()
            val avgDamage = acc.totalDamage.toDouble() / g
            val pickRate = acc.games.toDouble() / totalMatches
            val normalizedKda = avgKda / maxOf(1.0, overallAvgKda)
            val dmgShare = avgDamage / maxOf(1.0, overallAvgDamage)
            val tierScore = winRate * 0.5 + normalizedKda * 0.25 + dmgShare * 0.15 + pickRate * 0.10

            acc to tierScore
        }

        // 백분위 티어 분류 (minGames 이상인 챔피언만 대상)
        val qualifiedScores = scored
            .filter { (acc, _) -> acc.games >= minGames }
            .map { (_, score) -> score }
            .sorted()

        fun tierForScore(score: Double, qualified: Boolean): String {
            if (!qualified) return "?"
            if (qualifiedScores.isEmpty()) return "B"
            val rank = qualifiedScores.indexOfFirst { it >= score }.let { if (it == -1) qualifiedScores.size else it }
            val percentile = rank.toDouble() / qualifiedScores.size  // 0 = 최하위, 1 = 최상위
            val topPct = 1.0 - percentile
            return when {
                topPct <= 0.15 -> "S"
                topPct <= 0.35 -> "A"
                topPct <= 0.60 -> "B"
                topPct <= 0.80 -> "C"
                else           -> "D"
            }
        }

        val tierList = scored.map { (acc, tierScore) ->
            val g = acc.games.coerceAtLeast(1)
            val avgDamage = acc.totalDamage.toDouble() / g
            val avgKda = if (acc.deaths > 0) r2((acc.kills + acc.assists).toDouble() / acc.deaths)
                         else (acc.kills + acc.assists).toDouble()
            val pickRate = acc.games.toDouble() / totalMatches
            val qualified = acc.games >= minGames
            ChampionTierEntry(
                champion = acc.champion,
                championId = acc.championId,
                tier = tierForScore(tierScore, qualified),
                tierScore = r2(tierScore),
                games = acc.games,
                winRate = acc.wins * 100 / g,
                kda = avgKda,
                pickRate = r2(pickRate),
                avgDamage = r2(avgDamage),
            )
        }.sortedByDescending { it.tierScore }

        val byTier = tierList.groupBy { it.tier }

        ChampionTierResult(
            tierList = tierList,
            byTier = byTier,
            totalMatches = totalMatches,
        )
    }
}
