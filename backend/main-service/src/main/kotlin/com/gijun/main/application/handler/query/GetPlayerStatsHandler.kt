package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionStat
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.RecentMatchStat
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPlayerStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetPlayerStatsUseCase {

    override fun getPlayerStats(riotId: String, mode: String): PlayerDetailStatsResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class Entry(val matchId: String, val queueId: Int, val gameCreation: Long, val gameDuration: Int,
                         val champion: String, val championId: Int, val win: Boolean,
                         val kills: Int, val deaths: Int, val assists: Int,
                         val damage: Int, val cs: Int, val gold: Int, val visionScore: Int)

        val entries = matches.flatMap { m ->
            m.participants
                .filter { it.riotId == riotId }
                .map { p ->
                    Entry(m.matchId, m.queueId, m.gameCreation, m.gameDuration,
                          p.champion, p.championId, p.win,
                          p.kills, p.deaths, p.assists,
                          p.damage, p.cs, p.gold, p.visionScore)
                }
        }

        if (entries.isEmpty()) return PlayerDetailStatsResult(
            riotId = riotId, games = 0, wins = 0, losses = 0, winRate = 0,
            avgKills = 0.0, avgDeaths = 0.0, avgAssists = 0.0, kda = 0.0,
            avgDamage = 0, avgCs = 0.0, avgGold = 0, avgVisionScore = 0.0,
            championStats = emptyList(), recentMatches = emptyList()
        )

        val games = entries.size
        val wins = entries.count { it.win }
        val kills = entries.sumOf { it.kills }
        val deaths = entries.sumOf { it.deaths }
        val assists = entries.sumOf { it.assists }

        fun r1(v: Double) = (v * 10).toInt() / 10.0
        fun r2(v: Double) = (v * 100).toInt() / 100.0
        fun kda(k: Int, d: Int, a: Int) = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()

        val championStats = entries.groupBy { it.champion }.map { (champ, es) ->
            val cg = es.size; val cw = es.count { it.win }
            val ck = es.sumOf { it.kills }; val cd = es.sumOf { it.deaths }; val ca = es.sumOf { it.assists }
            ChampionStat(
                champion = champ, championId = es.first().championId,
                games = cg, wins = cw, winRate = cw * 100 / cg,
                avgKills = r1(ck.toDouble() / cg), avgDeaths = r1(cd.toDouble() / cg),
                avgAssists = r1(ca.toDouble() / cg), kda = kda(ck, cd, ca),
                avgDamage = es.sumOf { it.damage } / cg,
                avgCs = r1(es.sumOf { it.cs }.toDouble() / cg),
                avgGold = es.sumOf { it.gold } / cg,
            )
        }.sortedByDescending { it.games }

        val recentMatches = entries.sortedByDescending { it.gameCreation }.take(20).map {
            RecentMatchStat(matchId = it.matchId, champion = it.champion, championId = it.championId,
                win = it.win, kills = it.kills, deaths = it.deaths, assists = it.assists,
                damage = it.damage, cs = it.cs, gold = it.gold,
                gameCreation = it.gameCreation, gameDuration = it.gameDuration, queueId = it.queueId)
        }

        return PlayerDetailStatsResult(
            riotId = riotId, games = games, wins = wins, losses = games - wins,
            winRate = wins * 100 / games,
            avgKills = r1(kills.toDouble() / games), avgDeaths = r1(deaths.toDouble() / games),
            avgAssists = r1(assists.toDouble() / games), kda = kda(kills, deaths, assists),
            avgDamage = entries.sumOf { it.damage } / games,
            avgCs = r1(entries.sumOf { it.cs }.toDouble() / games),
            avgGold = entries.sumOf { it.gold } / games,
            avgVisionScore = r1(entries.sumOf { it.visionScore }.toDouble() / games),
            championStats = championStats, recentMatches = recentMatches,
        )
    }
}
