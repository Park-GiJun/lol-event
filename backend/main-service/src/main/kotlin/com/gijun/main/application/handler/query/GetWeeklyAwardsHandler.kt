package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.WeeklyAwardEntry
import com.gijun.main.application.dto.stats.result.WeeklyAwardsResult
import com.gijun.main.application.port.`in`.GetWeeklyAwardsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetWeeklyAwardsHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
) : GetWeeklyAwardsUseCase {
    override fun getWeeklyAwards(mode: String): WeeklyAwardsResult = cache.getOrCompute("weekly-awards:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        fun r2(v: Double) = (v * 100).toInt() / 100.0

        // 1. mostDeaths: 단일 경기 최다 사망
        data class SingleMatchDeaths(val riotId: String, val deaths: Int)
        val mostDeathsEntry = matches.flatMap { match ->
            match.participants.map { p -> SingleMatchDeaths(p.riotId, p.deaths) }
        }.maxByOrNull { it.deaths }?.let { entry ->
            WeeklyAwardEntry(
                riotId = entry.riotId,
                displayValue = entry.deaths.toString(),
                games = 1,
            )
        }

        // 플레이어별 집계 (게임 수, KDA 합계, 골드, 데미지, 승패, 항복 유발, 펜타킬 등)
        data class PlayerAcc(
            var games: Int = 0,
            var wins: Int = 0,
            var totalKills: Int = 0,
            var totalDeaths: Int = 0,
            var totalAssists: Int = 0,
            var totalGold: Int = 0,
            var totalDamage: Int = 0,
            var surrenderCount: Int = 0,
            var pentaKillTotal: Int = 0,
            var loneHeroCount: Int = 0,
            val championPlayCount: MutableMap<String, Int> = mutableMapOf(),
        )

        val accMap = mutableMapOf<String, PlayerAcc>()

        for (match in matches) {
            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                acc.games++
                if (p.win) acc.wins++
                acc.totalKills += p.kills
                acc.totalDeaths += p.deaths
                acc.totalAssists += p.assists
                acc.totalGold += p.gold
                acc.totalDamage += p.damage
                if (p.causedEarlySurrender) acc.surrenderCount++
                acc.pentaKillTotal += p.pentaKills
                // loneHero: 팀이 졌지만 개인 KDA >= 10
                if (!p.win) {
                    val kda = (p.kills + p.assists).toDouble() / maxOf(p.deaths, 1)
                    if (kda >= 10.0) acc.loneHeroCount++
                }
                acc.championPlayCount[p.champion] = (acc.championPlayCount[p.champion] ?: 0) + 1
            }
        }

        // 2. worstKda: 최소 5게임, 평균 KDA 최하위
        val worstKdaEntry = accMap.entries
            .filter { it.value.games >= 5 }
            .minByOrNull { (_, acc) ->
                (acc.totalKills + acc.totalAssists).toDouble() / maxOf(acc.totalDeaths, 1)
            }?.let { (riotId, acc) ->
                val kda = r2((acc.totalKills + acc.totalAssists).toDouble() / maxOf(acc.totalDeaths, 1))
                WeeklyAwardEntry(riotId = riotId, displayValue = kda.toString(), games = acc.games)
            }

        // 3. highGoldLowDamage: 골드 상위 30%인데 데미지 하위 30% ("먹튀")
        val allPlayers = accMap.entries.filter { it.value.games > 0 }
        val sortedByGold = allPlayers.sortedByDescending { it.value.totalGold }
        val sortedByDamage = allPlayers.sortedBy { it.value.totalDamage }
        val topGoldThreshold = maxOf(1, (allPlayers.size * 0.3).toInt())
        val lowDamageThreshold = maxOf(1, (allPlayers.size * 0.3).toInt())
        val topGoldRiotIds = sortedByGold.take(topGoldThreshold).map { it.key }.toSet()
        val lowDamageRiotIds = sortedByDamage.take(lowDamageThreshold).map { it.key }.toSet()
        val highGoldLowDamageCandidates = topGoldRiotIds.intersect(lowDamageRiotIds)
        val highGoldLowDamageEntry = highGoldLowDamageCandidates
            .mapNotNull { riotId -> accMap[riotId]?.let { riotId to it } }
            .minByOrNull { (_, acc) ->
                acc.totalDamage.toDouble() / maxOf(acc.totalGold, 1)
            }?.let { (riotId, acc) ->
                val ratio = r2(acc.totalDamage.toDouble() / maxOf(acc.totalGold, 1))
                WeeklyAwardEntry(riotId = riotId, displayValue = ratio.toString(), games = acc.games)
            }

        // 4. mostSurrenders: causedEarlySurrender 총 횟수 최다
        val mostSurrendersEntry = accMap.entries
            .filter { it.value.surrenderCount > 0 }
            .maxByOrNull { it.value.surrenderCount }
            ?.let { (riotId, acc) ->
                WeeklyAwardEntry(riotId = riotId, displayValue = acc.surrenderCount.toString(), games = acc.games)
            }

        // 5. pentaKillHero: 펜타킬 합산 최다
        val pentaKillHeroEntry = accMap.entries
            .filter { it.value.pentaKillTotal > 0 }
            .maxByOrNull { it.value.pentaKillTotal }
            ?.let { (riotId, acc) ->
                WeeklyAwardEntry(riotId = riotId, displayValue = acc.pentaKillTotal.toString(), games = acc.games)
            }

        // 6. loneHero: 팀이 졌지만 개인 KDA 10 이상인 경기 횟수 최다
        val loneHeroEntry = accMap.entries
            .filter { it.value.loneHeroCount > 0 }
            .maxByOrNull { it.value.loneHeroCount }
            ?.let { (riotId, acc) ->
                WeeklyAwardEntry(riotId = riotId, displayValue = acc.loneHeroCount.toString(), games = acc.games)
            }

        // 7. highestWinRate: 최소 5게임 중 최고 승률
        val highestWinRateEntry = accMap.entries
            .filter { it.value.games >= 5 }
            .maxByOrNull { it.value.wins.toDouble() / it.value.games }
            ?.let { (riotId, acc) ->
                val winRate = r2(acc.wins.toDouble() / acc.games * 100)
                WeeklyAwardEntry(riotId = riotId, displayValue = "$winRate%", games = acc.games)
            }

        // 8. mostGamesChampion: 단일 챔피언 최다 플레이 (챔피언명 포함)
        data class ChampionPlayEntry(val riotId: String, val champion: String, val count: Int, val totalGames: Int)
        val mostGamesChampionEntry = accMap.entries
            .flatMap { (riotId, acc) ->
                acc.championPlayCount.entries.map { (champ, cnt) ->
                    ChampionPlayEntry(riotId, champ, cnt, acc.games)
                }
            }
            .maxByOrNull { it.count }
            ?.let { entry ->
                WeeklyAwardEntry(
                    riotId = entry.riotId,
                    displayValue = "${entry.champion}(${entry.count})",
                    games = entry.totalGames,
                )
            }

        WeeklyAwardsResult(
            mostDeaths = mostDeathsEntry,
            worstKda = worstKdaEntry,
            highGoldLowDamage = highGoldLowDamageEntry,
            mostSurrenders = mostSurrendersEntry,
            pentaKillHero = pentaKillHeroEntry,
            loneHero = loneHeroEntry,
            highestWinRate = highestWinRateEntry,
            mostGamesChampion = mostGamesChampionEntry,
        )
    }
}
