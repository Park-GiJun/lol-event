package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.VisionDominanceResult
import com.gijun.main.application.dto.stats.result.VisionPlayerEntry
import com.gijun.main.application.port.`in`.GetVisionDominanceUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetVisionDominanceHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetVisionDominanceUseCase {

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getVisionDominance(mode: String): VisionDominanceResult = cache.getOrCompute("vision-dominance:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class Acc(
            var games: Int = 0,
            var totalVisionScore: Long = 0,
            var totalWardsPlaced: Long = 0,
            var totalWardsKilled: Long = 0,
            var totalSightWardsBought: Long = 0,
            var totalControlWardsBought: Long = 0,
        )

        val accMap = mutableMapOf<String, Acc>()

        for (match in matches) {
            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { Acc() }
                acc.games++
                acc.totalVisionScore += p.visionScore
                acc.totalWardsPlaced += p.wardsPlaced
                acc.totalWardsKilled += p.wardsKilled
                acc.totalSightWardsBought += p.sightWardsBoughtInGame
                acc.totalControlWardsBought += p.visionWardsBoughtInGame
            }
        }

        val players = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                val totalWards = (acc.totalWardsPlaced + acc.totalWardsKilled).coerceAtLeast(1)
                VisionPlayerEntry(
                    riotId = riotId,
                    games = acc.games,
                    avgVisionScore = r2(acc.totalVisionScore / g),
                    avgWardsPlaced = r2(acc.totalWardsPlaced / g),
                    avgWardsKilled = r2(acc.totalWardsKilled / g),
                    avgSightWardsBought = r2(acc.totalSightWardsBought / g),
                    avgControlWardsBought = r2(acc.totalControlWardsBought / g),
                    wardKillRate = r2(acc.totalWardsKilled.toDouble() / totalWards),
                )
            }
            .sortedByDescending { it.avgVisionScore }

        VisionDominanceResult(players = players)
    }
}
