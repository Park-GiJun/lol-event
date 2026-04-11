package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.SupportImpactEntry
import com.gijun.main.application.dto.stats.result.SupportImpactResult
import com.gijun.main.application.port.`in`.GetSupportImpactUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetSupportImpactHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetSupportImpactUseCase {

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getSupportImpact(mode: String): SupportImpactResult = cache.getOrCompute("support-impact:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayerAcc(
            var games: Int = 0,
            var totalHealShare: Double = 0.0,
            var totalCcShare: Double = 0.0,
            var totalVisionShare: Double = 0.0,
            var totalShieldProxy: Double = 0.0,
            val champions: MutableMap<String, Int> = mutableMapOf(),
            val championIds: MutableMap<String, Int> = mutableMapOf(),
        )

        val accMap = mutableMapOf<String, PlayerAcc>()

        for (match in matches) {
            val byTeam = match.participants.groupBy { it.teamId }
            val teamHeal = byTeam.mapValues { (_, ps) -> ps.sumOf { it.totalHeal } }
            val teamCc = byTeam.mapValues { (_, ps) -> ps.sumOf { it.timeCCingOthers } }
            val teamVision = byTeam.mapValues { (_, ps) -> ps.sumOf { it.visionScore } }

            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }

                val teamHealTotal = teamHeal[p.teamId] ?: 1
                val teamCcTotal = teamCc[p.teamId] ?: 1
                val teamVisionTotal = teamVision[p.teamId] ?: 1

                val healShare = p.totalHeal.toDouble() / maxOf(1, teamHealTotal)
                val ccShare = p.timeCCingOthers.toDouble() / maxOf(1, teamCcTotal)
                val visionShare = p.visionScore.toDouble() / maxOf(1, teamVisionTotal)
                val shieldProxy = p.damageSelfMitigated.toDouble() / maxOf(1, p.totalDamageTaken)

                acc.games++
                acc.totalHealShare += healShare
                acc.totalCcShare += ccShare
                acc.totalVisionShare += visionShare
                acc.totalShieldProxy += shieldProxy
                acc.champions[p.champion] = (acc.champions[p.champion] ?: 0) + 1
                acc.championIds[p.champion] = p.championId
            }
        }

        val rankings = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                val avgHealShare = acc.totalHealShare / g
                val avgCcShare = acc.totalCcShare / g
                val avgVisionShare = acc.totalVisionShare / g
                val avgShieldProxy = acc.totalShieldProxy / g

                val supportImpact =
                    avgHealShare * 0.30 +
                    avgCcShare * 0.30 +
                    avgVisionShare * 0.25 +
                    avgShieldProxy * 0.15

                val roleTag = when {
                    avgHealShare > 0.4 -> "팀 힐러"
                    avgCcShare > 0.4 -> "CC 머신"
                    avgVisionShare > 0.4 -> "시야 지배자"
                    else -> "밸런스 서포터"
                }

                val topChampion = acc.champions.maxByOrNull { it.value }?.key

                SupportImpactEntry(
                    riotId = riotId,
                    games = acc.games,
                    avgHealShare = r2(avgHealShare),
                    avgCcShare = r2(avgCcShare),
                    avgVisionShare = r2(avgVisionShare),
                    avgShieldProxy = r2(avgShieldProxy),
                    supportImpact = r2(supportImpact),
                    roleTag = roleTag,
                    topChampion = topChampion,
                    topChampionId = topChampion?.let { acc.championIds[it] },
                )
            }
            .sortedByDescending { it.supportImpact }

        SupportImpactResult(rankings = rankings)
    }
}
