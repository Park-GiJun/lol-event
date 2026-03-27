package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.MetaShiftChampion
import com.gijun.main.application.dto.stats.result.MetaShiftResult
import com.gijun.main.application.port.`in`.GetMetaShiftUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMetaShiftHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetMetaShiftUseCase {

    private fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getMetaShift(mode: String): MetaShiftResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        if (matches.isEmpty()) {
            return MetaShiftResult(
                risingChampions = emptyList(),
                fallingChampions = emptyList(),
                stableTopChampions = emptyList(),
                totalMatchesAnalyzed = 0,
            )
        }

        // Sort matches by gameCreation ascending
        val sortedMatches = matches.sortedBy { it.gameCreation }
        val totalMatches = sortedMatches.size
        val halfIndex = totalMatches / 2

        val olderMatches = sortedMatches.take(halfIndex)
        val recentMatches = sortedMatches.drop(halfIndex)

        val olderCount = olderMatches.size.coerceAtLeast(1)
        val recentCount = recentMatches.size.coerceAtLeast(1)

        // Count champion picks and wins per period
        data class ChampionStats(
            val champion: String,
            val championId: Int,
            var totalPicks: Int = 0,
            var totalWins: Int = 0,
            var olderPicks: Int = 0,
            var recentPicks: Int = 0,
        )

        val champMap = mutableMapOf<Int, ChampionStats>()

        for (match in olderMatches) {
            for (p in match.participants) {
                val cs = champMap.getOrPut(p.championId) { ChampionStats(p.champion, p.championId) }
                cs.totalPicks++
                cs.olderPicks++
                if (p.win) cs.totalWins++
            }
        }

        for (match in recentMatches) {
            for (p in match.participants) {
                val cs = champMap.getOrPut(p.championId) { ChampionStats(p.champion, p.championId) }
                cs.totalPicks++
                cs.recentPicks++
                if (p.win) cs.totalWins++
            }
        }

        val allChampions = champMap.values.map { cs ->
            val pickRate = r2(cs.totalPicks.toDouble() / totalMatches)
            val recentPickRate = r2(cs.recentPicks.toDouble() / recentCount)
            val olderPickRate = r2(cs.olderPicks.toDouble() / olderCount)
            val trend = r2(recentPickRate - olderPickRate)
            val winRate = r2(if (cs.totalPicks > 0) cs.totalWins.toDouble() / cs.totalPicks * 100 else 0.0)

            val metaTag = when {
                trend > 0.05 -> "상승 메타"
                trend < -0.05 -> "하락 메타"
                else -> "안정 메타"
            }

            MetaShiftChampion(
                champion = cs.champion,
                championId = cs.championId,
                totalGames = cs.totalPicks,
                pickRate = pickRate,
                recentPickRate = recentPickRate,
                olderPickRate = olderPickRate,
                trend = trend,
                winRate = winRate,
                metaTag = metaTag,
            )
        }

        val risingChampions = allChampions
            .filter { it.metaTag == "상승 메타" }
            .sortedByDescending { it.trend }
            .take(5)

        val fallingChampions = allChampions
            .filter { it.metaTag == "하락 메타" }
            .sortedBy { it.trend }
            .take(5)

        val stableTopChampions = allChampions
            .filter { it.metaTag == "안정 메타" }
            .sortedByDescending { it.pickRate }
            .take(10)

        return MetaShiftResult(
            risingChampions = risingChampions,
            fallingChampions = fallingChampions,
            stableTopChampions = stableTopChampions,
            totalMatchesAnalyzed = totalMatches,
        )
    }
}
