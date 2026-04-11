package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.LaneLeaderboardResult
import com.gijun.main.application.dto.stats.result.PlayerLaneStat
import com.gijun.main.application.port.`in`.GetLaneLeaderboardUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetLaneLeaderboardHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetLaneLeaderboardUseCase {

    private fun resolvePosition(lane: String?, role: String?, neutralMinionsKilled: Int): String? = when {
        lane == "TOP"                                                          -> "TOP"
        lane == "JUNGLE" && neutralMinionsKilled >= 30                        -> "JUNGLE"
        lane == "MID" || lane == "MIDDLE"                                     -> "MID"
        lane == "BOTTOM" && (role == "DUO_SUPPORT" || role == "SUPPORT")      -> "SUPPORT"
        lane == "BOTTOM"                                                       -> "BOTTOM"
        else                                                                   -> null
    }

    private fun r1(v: Double) = (v * 10).toInt() / 10.0
    private fun r2(v: Double) = (v * 100).toInt() / 100.0
    private fun kda(k: Int, d: Int, a: Int) = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()

    override fun getLaneLeaderboard(lane: String, mode: String): LaneLeaderboardResult = cache.getOrCompute("lane-leaderboard:$lane:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class Entry(
            val riotId: String, val champion: String, val championId: Int, val win: Boolean,
            val kills: Int, val deaths: Int, val assists: Int,
            val damage: Int, val cs: Int, val gold: Int, val visionScore: Int,
            val damageTaken: Int, val objectiveDamage: Int,
            val wardsPlaced: Int, val ccTime: Int, val neutralMinions: Int,
        )

        val entries = matches.flatMap { m ->
            m.participants.mapNotNull { p ->
                val pos = resolvePosition(p.lane, p.role, p.neutralMinionsKilled)
                if (pos != lane) return@mapNotNull null
                Entry(
                    riotId = p.riotId, champion = p.champion, championId = p.championId, win = p.win,
                    kills = p.kills, deaths = p.deaths, assists = p.assists,
                    damage = p.damage, cs = p.cs, gold = p.gold, visionScore = p.visionScore,
                    damageTaken = p.totalDamageTaken, objectiveDamage = p.damageDealtToObjectives,
                    wardsPlaced = p.wardsPlaced, ccTime = p.timeCCingOthers,
                    neutralMinions = p.neutralMinionsKilled,
                )
            }
        }

        val players = entries.groupBy { it.riotId }.map { (riotId, es) ->
            val g = es.size; val w = es.count { it.win }
            val k = es.sumOf { it.kills }; val d = es.sumOf { it.deaths }; val a = es.sumOf { it.assists }
            val topChampEntry = es.groupBy { it.championId }.maxByOrNull { it.value.size }
            PlayerLaneStat(
                riotId = riotId,
                games = g, wins = w, winRate = w * 100 / g,
                avgKills   = r1(k.toDouble() / g),
                avgDeaths  = r1(d.toDouble() / g),
                avgAssists = r1(a.toDouble() / g),
                kda        = kda(k, d, a),
                avgDamage        = es.sumOf { it.damage } / g,
                avgCs            = r1(es.sumOf { it.cs }.toDouble() / g),
                avgGold          = es.sumOf { it.gold } / g,
                avgVisionScore   = r1(es.sumOf { it.visionScore }.toDouble() / g),
                avgDamageTaken   = es.sumOf { it.damageTaken } / g,
                avgObjectiveDamage = es.sumOf { it.objectiveDamage } / g,
                avgWardsPlaced   = r1(es.sumOf { it.wardsPlaced }.toDouble() / g),
                avgCcTime        = r1(es.sumOf { it.ccTime }.toDouble() / g),
                avgNeutralMinions = r1(es.sumOf { it.neutralMinions }.toDouble() / g),
                topChampion   = topChampEntry?.value?.first()?.champion,
                topChampionId = topChampEntry?.key,
            )
        }.sortedByDescending { it.games }

        LaneLeaderboardResult(lane = lane, players = players)
    }
}
