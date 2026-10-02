package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.VisionDominanceResult
import com.gijun.main.application.dto.result.VisionPlayerEntry
import com.gijun.main.application.port.`in`.GetVisionDominanceUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetVisionDominanceHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetVisionDominanceUseCase {
    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getVisionDominance(mode: String): VisionDominanceResult =
        statsResultCacheQueryPort.getOrCompute("vision-dominance:$mode") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

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

            val players =
                accMap.entries
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
                            avgControlWardsBought = r2(acc.totalControlWardsBought / g),
                            wardKillRate = r2(acc.totalWardsKilled.toDouble() / totalWards),
                        )
                    }.sortedByDescending { it.avgVisionScore }

            VisionDominanceResult(players = players)
        }
}
