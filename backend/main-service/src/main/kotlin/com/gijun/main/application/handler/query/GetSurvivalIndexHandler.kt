package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.SurvivalIndexEntry
import com.gijun.main.application.dto.stats.result.SurvivalIndexResult
import com.gijun.main.application.port.`in`.GetSurvivalIndexUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetSurvivalIndexHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetSurvivalIndexUseCase {

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getSurvivalIndex(mode: String): SurvivalIndexResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayerAcc(
            var games: Int = 0,
            var totalDamageTaken: Double = 0.0,
            var totalSelfMitigated: Double = 0.0,
            var totalMitigationRatio: Double = 0.0,
            var totalTankShare: Double = 0.0,
            var totalSurvivalRatio: Double = 0.0,
            var totalDeaths: Double = 0.0,
        )

        val accMap = mutableMapOf<String, PlayerAcc>()

        for (match in matches) {
            val byTeam = match.participants.groupBy { it.teamId }
            val teamDamageTaken = byTeam.mapValues { (_, ps) -> ps.sumOf { it.totalDamageTaken } }

            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                val teamTotalDamageTaken = teamDamageTaken[p.teamId] ?: 1

                val mitigationRatio = p.damageSelfMitigated.toDouble() / maxOf(1, p.totalDamageTaken)
                val tankShare = p.totalDamageTaken.toDouble() / maxOf(1, teamTotalDamageTaken)
                val survivalRatio = p.longestTimeSpentLiving.toDouble() / maxOf(1, match.gameDuration)

                acc.games++
                acc.totalDamageTaken += p.totalDamageTaken
                acc.totalSelfMitigated += p.damageSelfMitigated
                acc.totalMitigationRatio += mitigationRatio
                acc.totalTankShare += tankShare
                acc.totalSurvivalRatio += survivalRatio
                acc.totalDeaths += p.deaths
            }
        }

        val rankings = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                val avgMitigationRatio = acc.totalMitigationRatio / g
                val avgTankShare = acc.totalTankShare / g
                val avgSurvivalRatio = acc.totalSurvivalRatio / g
                val avgDeaths = acc.totalDeaths / g

                val survivalIndex =
                    avgMitigationRatio * 0.35 +
                    avgTankShare * 0.30 +
                    avgSurvivalRatio * 0.20 +
                    (1.0 / maxOf(1.0, avgDeaths)) * 0.15

                SurvivalIndexEntry(
                    riotId = riotId,
                    games = acc.games,
                    avgDamageTaken = r2(acc.totalDamageTaken / g),
                    avgSelfMitigated = r2(acc.totalSelfMitigated / g),
                    avgMitigationRatio = r2(avgMitigationRatio),
                    avgTankShare = r2(avgTankShare),
                    avgSurvivalRatio = r2(avgSurvivalRatio),
                    avgDeaths = r2(avgDeaths),
                    survivalIndex = r2(survivalIndex),
                )
            }
            .sortedByDescending { it.survivalIndex }

        return SurvivalIndexResult(rankings = rankings)
    }
}
