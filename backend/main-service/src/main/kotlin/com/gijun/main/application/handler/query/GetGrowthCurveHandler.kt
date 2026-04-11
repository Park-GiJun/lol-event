package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.GrowthCurveEntry
import com.gijun.main.application.dto.stats.result.GrowthCurveResult
import com.gijun.main.application.port.`in`.GetGrowthCurveUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetGrowthCurveHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetGrowthCurveUseCase {

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    private fun rollingAvg(list: List<Double>, index: Int, window: Int = 5): Double {
        val from = maxOf(0, index - window + 1)
        val slice = list.subList(from, index + 1)
        return if (slice.isEmpty()) 0.0 else slice.average()
    }

    override fun getGrowthCurve(riotId: String, mode: String): GrowthCurveResult = cache.getOrCompute("growth-curve:$riotId:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class RawEntry(
            val matchId: String,
            val gameCreation: Long,
            val champion: String,
            val win: Boolean,
            val kda: Double,
            val dmgShare: Double,
            val visionPerMin: Double,
            val csPerMin: Double,
        )

        val rawEntries = matches
            .mapNotNull { match ->
                val p = match.participants.find { it.riotId == riotId } ?: return@mapNotNull null
                val durationMin = maxOf(1.0, match.gameDuration / 60.0)
                val teamTotalDamage = match.participants
                    .filter { it.teamId == p.teamId }
                    .sumOf { it.damage }

                val kda = (p.kills + p.assists).toDouble() / maxOf(1, p.deaths)
                val dmgShare = p.damage.toDouble() / maxOf(1, teamTotalDamage)
                val visionPerMin = p.visionScore / durationMin
                val csPerMin = p.cs / durationMin

                RawEntry(
                    matchId = match.matchId,
                    gameCreation = match.gameCreation,
                    champion = p.champion,
                    win = p.win,
                    kda = kda,
                    dmgShare = dmgShare,
                    visionPerMin = visionPerMin,
                    csPerMin = csPerMin,
                )
            }
            .sortedBy { it.gameCreation }

        val kdaList = rawEntries.map { it.kda }
        val dmgShareList = rawEntries.map { it.dmgShare }
        val csPerMinList = rawEntries.map { it.csPerMin }

        val entries = rawEntries.mapIndexed { idx, raw ->
            GrowthCurveEntry(
                matchId = raw.matchId,
                gameCreation = raw.gameCreation,
                champion = raw.champion,
                win = raw.win,
                kda = r2(raw.kda),
                dmgShare = r2(raw.dmgShare),
                visionPerMin = r2(raw.visionPerMin),
                csPerMin = r2(raw.csPerMin),
                rollingKda = r2(rollingAvg(kdaList, idx)),
                rollingDmgShare = r2(rollingAvg(dmgShareList, idx)),
                rollingCsPerMin = r2(rollingAvg(csPerMinList, idx)),
            )
        }

        val overallAvgKda = if (kdaList.isEmpty()) 0.0 else r2(kdaList.average())
        val recentKdaList = kdaList.takeLast(5)
        val recentAvgKda = if (recentKdaList.isEmpty()) 0.0 else r2(recentKdaList.average())

        val trend = when {
            overallAvgKda <= 0.0 -> "STABLE"
            recentAvgKda >= overallAvgKda * 1.10 -> "IMPROVING"
            recentAvgKda <= overallAvgKda * 0.90 -> "DECLINING"
            else -> "STABLE"
        }

        GrowthCurveResult(
            riotId = riotId,
            entries = entries,
            totalGames = entries.size,
            recentAvgKda = recentAvgKda,
            overallAvgKda = overallAvgKda,
            trend = trend,
        )
    }
}
