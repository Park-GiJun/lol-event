package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChaosMatchEntry
import com.gijun.main.application.dto.stats.result.ChaosMatchResult
import com.gijun.main.application.port.`in`.GetChaosMatchUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetChaosMatchHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
) : GetChaosMatchUseCase {
    override fun getChaosMatch(mode: String): ChaosMatchResult = cache.getOrCompute("chaos-match:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        fun r2(v: Double) = (v * 100).toInt() / 100.0

        if (matches.isEmpty()) {
            return@getOrCompute ChaosMatchResult(
                topChaosMatches      = emptyList(),
                topBloodBathMatches  = emptyList(),
                topStrategicMatches  = emptyList(),
                avgChaosIndex        = 0.0,
            )
        }

        // 1단계: 각 매치의 kill_density 계산 (평균 산출용)
        data class MatchStats(
            val matchId: String,
            val gameCreation: Long,
            val durationMin: Double,
            val totalKills: Int,
            val killDensity: Double,
            val multiKillScore: Int,
            val totalCcTime: Int,
            val hasSurrender: Boolean,
            val teamGolds: Map<Int, Int>,
            val participants: List<String>,
        )

        val matchStatsList = matches.map { match ->
            val durationMin = maxOf(match.gameDuration / 60.0, 1.0)
            val totalKills = match.participants.sumOf { it.kills }
            val killDensity = totalKills / durationMin
            val multiKillScore =
                match.participants.sumOf { p ->
                    p.doubleKills * 1 + p.tripleKills * 3 + p.quadraKills * 6 + p.pentaKills * 10
                }
            val totalCcTime = match.participants.sumOf { it.timeCCingOthers }
            val hasSurrender = match.participants.any { it.gameEndedInEarlySurrender }
            val teamGolds = match.participants.groupBy { it.teamId }
                .mapValues { (_, ps) -> ps.sumOf { it.gold } }
            val participantIds = match.participants.map { it.riotId }

            MatchStats(
                matchId        = match.matchId,
                gameCreation   = match.gameCreation,
                durationMin    = durationMin,
                totalKills     = totalKills,
                killDensity    = killDensity,
                multiKillScore = multiKillScore,
                totalCcTime    = totalCcTime,
                hasSurrender   = hasSurrender,
                teamGolds      = teamGolds,
                participants   = participantIds,
            )
        }

        val avgKillDensity = matchStatsList.map { it.killDensity }.average()

        val chaosEntries = matchStatsList.map { ms ->
            val ccPerMin = ms.totalCcTime / ms.durationMin
            val surrenderPenalty = if (ms.hasSurrender) 0.6 else 1.0
            val chaosIndex =
                (ms.killDensity * 3.0 + ms.multiKillScore * 2.0 + ccPerMin * 0.3) * surrenderPenalty

            val goldValues = ms.teamGolds.values.toList()
            val goldRatio = if (goldValues.size >= 2) {
                val minGold = goldValues.min()
                val maxGold = goldValues.max()
                minGold.toDouble() / maxOf(maxGold, 1)
            } else 1.0

            val gameTypeTag = when {
                ms.killDensity > avgKillDensity && goldRatio > 0.88  -> "혈전"
                ms.killDensity > avgKillDensity && goldRatio <= 0.75  -> "학살"
                ms.killDensity <= avgKillDensity && goldRatio > 0.88  -> "운영 접전"
                else                                                   -> "일반"
            }

            ChaosMatchEntry(
                matchId         = ms.matchId,
                gameCreation    = ms.gameCreation,
                gameDurationMin = r2(ms.durationMin),
                chaosIndex      = r2(chaosIndex),
                totalKills      = ms.totalKills,
                killDensity     = r2(ms.killDensity),
                multiKillScore  = ms.multiKillScore,
                gameTypeTag     = gameTypeTag,
                participants    = ms.participants,
            )
        }

        val topChaosMatches     = chaosEntries.sortedByDescending { it.chaosIndex }.take(10)
        val topBloodBathMatches = chaosEntries.filter { it.gameTypeTag == "학살" }.sortedByDescending { it.chaosIndex }.take(5)
        val topStrategicMatches = chaosEntries.filter { it.gameTypeTag == "운영 접전" }.sortedByDescending { it.chaosIndex }.take(5)
        val avgChaosIndex       = r2(chaosEntries.map { it.chaosIndex }.average())

        ChaosMatchResult(
            topChaosMatches      = topChaosMatches,
            topBloodBathMatches  = topBloodBathMatches,
            topStrategicMatches  = topStrategicMatches,
            avgChaosIndex        = avgChaosIndex,
        )
    }
}
