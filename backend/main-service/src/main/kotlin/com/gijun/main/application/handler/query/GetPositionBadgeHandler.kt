package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.PositionBadgeEntry
import com.gijun.main.application.dto.stats.result.PositionBadgeResult
import com.gijun.main.application.port.`in`.GetPositionBadgeUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetPositionBadgeHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetPositionBadgeUseCase {

    private fun resolvePosition(lane: String?, role: String?, neutralMinionsKilled: Int): String? = when {
        lane == "TOP" -> "TOP"
        neutralMinionsKilled >= 30 -> "JUNGLE"
        lane == "MIDDLE" || lane == "MID" -> "MID"
        lane == "BOTTOM" && (role?.contains("SUPPORT") == true || role == "DUO_SUPPORT") -> "SUPPORT"
        lane == "BOTTOM" && role?.contains("CARRY") == true -> "BOTTOM"
        else -> null
    }

    private fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getPositionBadge(mode: String): PositionBadgeResult = cache.getOrCompute("position-badge:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class ParticipantRecord(
            val riotId: String,
            val champion: String,
            val championId: Int,
            val win: Boolean,
            val kills: Int,
            val deaths: Int,
            val assists: Int,
            val damage: Int,
            val visionScore: Int,
            val gameDuration: Int,
        )

        val positions = listOf("TOP", "JUNGLE", "MID", "BOTTOM", "SUPPORT")
        val positionData = positions.associateWith { mutableListOf<ParticipantRecord>() }

        for (match in matches) {
            for (p in match.participants) {
                val pos = resolvePosition(p.lane, p.role, p.neutralMinionsKilled) ?: continue
                positionData[pos]?.add(
                    ParticipantRecord(
                        riotId = p.riotId,
                        champion = p.champion,
                        championId = p.championId,
                        win = p.win,
                        kills = p.kills,
                        deaths = p.deaths,
                        assists = p.assists,
                        damage = p.damage,
                        visionScore = p.visionScore,
                        gameDuration = match.gameDuration,
                    )
                )
            }
        }

        val allPositionRankings = mutableMapOf<String, List<PositionBadgeEntry>>()

        for (pos in positions) {
            val records = positionData[pos] ?: emptyList()
            val avgDamageOverall = if (records.isNotEmpty()) records.map { it.damage }.average() else 1.0

            val playerEntries = records.groupBy { it.riotId }.mapNotNull { (riotId, es) ->
                if (es.size < 3) return@mapNotNull null
                val g = es.size
                val w = es.count { it.win }
                val winRate = w.toDouble() / g
                val k = es.sumOf { it.kills }
                val d = es.sumOf { it.deaths }
                val a = es.sumOf { it.assists }
                val kda = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()
                val avgDamage = es.map { it.damage }.average()
                val avgVisionPerMin = es.map { rec ->
                    val durationMin = rec.gameDuration / 60.0
                    rec.visionScore / maxOf(1.0, durationMin)
                }.average()

                val damageShare = avgDamage / maxOf(1.0, avgDamageOverall)
                val positionScore = winRate * 0.3 + kda * 0.25 + damageShare * 0.25 + avgVisionPerMin * 0.2

                val topChampEntry = es.groupBy { it.championId }.maxByOrNull { it.value.size }

                PositionBadgeEntry(
                    position = pos,
                    riotId = riotId,
                    games = g,
                    winRate = r2(winRate * 100),
                    kda = kda,
                    avgDamage = r2(avgDamage),
                    positionScore = r2(positionScore),
                    topChampion = topChampEntry?.value?.first()?.champion,
                    topChampionId = topChampEntry?.key,
                )
            }.sortedByDescending { it.positionScore }

            allPositionRankings[pos] = playerEntries
        }

        val topPositions = positions.mapNotNull { pos ->
            allPositionRankings[pos]?.firstOrNull()
        }

        PositionBadgeResult(
            topPositions = topPositions,
            allPositionRankings = allPositionRankings,
        )
    }
}
