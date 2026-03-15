package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionPlayerStat
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetChampionStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetChampionStatsUseCase {

    override fun getChampionStats(champion: String, mode: String): ChampionDetailStats {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        // champion 이름과 일치하는 참가자 수집 (대소문자 무시)
        val pairs = matches.flatMap { m ->
            m.participants
                .filter { it.champion.equals(champion, ignoreCase = true) }
                .map { it to m }
        }

        if (pairs.isEmpty()) return ChampionDetailStats(
            champion = champion, championId = 0,
            totalGames = 0, totalWins = 0, winRate = 0,
            players = emptyList(),
        )

        val championName = pairs.first().first.champion
        val championId   = pairs.first().first.championId
        val totalGames   = pairs.size
        val totalWins    = pairs.count { (p, _) -> p.win }

        fun r1(v: Double) = (v * 10).toInt() / 10.0
        fun r2(v: Double) = (v * 100).toInt() / 100.0
        fun kda(k: Int, d: Int, a: Int) = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()

        val players = pairs
            .groupBy { (p, _) -> p.riotId }
            .map { (riotId, entries) ->
                val gs = entries.size
                val ws = entries.count { (p, _) -> p.win }
                val k  = entries.sumOf { (p, _) -> p.kills }
                val d  = entries.sumOf { (p, _) -> p.deaths }
                val a  = entries.sumOf { (p, _) -> p.assists }
                ChampionPlayerStat(
                    riotId         = riotId,
                    games          = gs,
                    wins           = ws,
                    winRate        = ws * 100 / gs,
                    avgKills       = r1(k.toDouble() / gs),
                    avgDeaths      = r1(d.toDouble() / gs),
                    avgAssists     = r1(a.toDouble() / gs),
                    kda            = kda(k, d, a),
                    avgDamage      = entries.sumOf { (p, _) -> p.damage } / gs,
                    avgCs          = r1(entries.sumOf { (p, _) -> p.cs }.toDouble() / gs),
                    avgGold        = entries.sumOf { (p, _) -> p.gold } / gs,
                    avgVisionScore = r1(entries.sumOf { (p, _) -> p.visionScore }.toDouble() / gs),
                )
            }
            .sortedByDescending { it.games }

        return ChampionDetailStats(
            champion   = championName,
            championId = championId,
            totalGames = totalGames,
            totalWins  = totalWins,
            winRate    = totalWins * 100 / totalGames,
            players    = players,
        )
    }
}
