package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.JungleDominanceEntry
import com.gijun.main.application.dto.stats.result.JungleDominanceResult
import com.gijun.main.application.port.`in`.GetJungleDominanceUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetJungleDominanceHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetJungleDominanceUseCase {

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getJungleDominance(mode: String): JungleDominanceResult = cache.getOrCompute("jungle-dominance:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayerAcc(
            var games: Int = 0,
            var totalInvadeRatio: Double = 0.0,
            var totalObjShare: Double = 0.0,
            var totalKp: Double = 0.0,
            var totalJungleCs: Double = 0.0,
            var totalVisionPerMin: Double = 0.0,
            val champions: MutableMap<String, Int> = mutableMapOf(),
            val championIds: MutableMap<String, Int> = mutableMapOf(),
        )

        val accMap = mutableMapOf<String, PlayerAcc>()

        for (match in matches) {
            val byTeam = match.participants.groupBy { it.teamId }
            val teamKills = byTeam.mapValues { (_, ps) -> ps.sumOf { it.kills } }
            val teamObjDamage = byTeam.mapValues { (_, ps) -> ps.sumOf { it.damageDealtToObjectives } }

            val durationMin = maxOf(1.0, match.gameDuration / 60.0)
            val junglers = match.participants.filter { it.neutralMinionsKilled >= 30 }

            for (p in junglers) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                val teamKillsTotal = teamKills[p.teamId] ?: 1
                val teamObjTotal = teamObjDamage[p.teamId] ?: 1

                val invadeRatio = p.neutralMinionsKilledEnemyJungle.toDouble() / maxOf(1, p.neutralMinionsKilled)
                val objShare = p.damageDealtToObjectives.toDouble() / maxOf(1, teamObjTotal)
                val kp = (p.kills + p.assists).toDouble() / maxOf(1, teamKillsTotal)
                val visionPerMin = p.visionScore / durationMin

                acc.games++
                acc.totalInvadeRatio += invadeRatio
                acc.totalObjShare += objShare
                acc.totalKp += kp
                acc.totalJungleCs += p.neutralMinionsKilled.toDouble()
                acc.totalVisionPerMin += visionPerMin
                acc.champions[p.champion] = (acc.champions[p.champion] ?: 0) + 1
                acc.championIds[p.champion] = p.championId
            }
        }

        val rankings = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                val avgInvadeRatio = acc.totalInvadeRatio / g
                val avgObjShare = acc.totalObjShare / g
                val avgKp = acc.totalKp / g
                val avgJungleCs = acc.totalJungleCs / g
                val avgVisionPerMin = acc.totalVisionPerMin / g

                val jungleDominance =
                    avgInvadeRatio * 0.25 +
                    avgObjShare * 0.30 +
                    avgKp * 0.30 +
                    avgVisionPerMin * 0.15

                val playStyleTag = when {
                    avgInvadeRatio > 0.15 && avgKp > 0.5 -> "공격형"
                    avgObjShare > 0.35 -> "오브젝트 특화"
                    avgKp > 0.6 && avgInvadeRatio < 0.1 -> "안전 갱킹형"
                    else -> "밸런스형"
                }

                val topChampion = acc.champions.maxByOrNull { it.value }?.key

                JungleDominanceEntry(
                    riotId = riotId,
                    games = acc.games,
                    avgInvadeRatio = r2(avgInvadeRatio),
                    avgObjShare = r2(avgObjShare),
                    avgKp = r2(avgKp),
                    avgJungleCs = r2(avgJungleCs),
                    avgJungleDominance = r2(jungleDominance),
                    playStyleTag = playStyleTag,
                    topChampion = topChampion,
                    topChampionId = topChampion?.let { acc.championIds[it] },
                )
            }
            .sortedByDescending { it.avgJungleDominance }

        JungleDominanceResult(rankings = rankings)
    }
}
