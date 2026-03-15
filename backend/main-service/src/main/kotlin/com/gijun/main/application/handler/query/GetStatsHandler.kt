package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionCount
import com.gijun.main.application.dto.stats.result.PlayerStatsResult
import com.gijun.main.application.dto.stats.result.StatsResult
import com.gijun.main.application.port.`in`.GetStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.MemberPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val memberPersistencePort: MemberPersistencePort
) : GetStatsUseCase {

    override fun getStats(mode: String): StatsResult {
        val members = memberPersistencePort.findAll()
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class Acc(
            val riotId: String,
            var wins: Int = 0, var losses: Int = 0,
            var kills: Int = 0, var deaths: Int = 0, var assists: Int = 0,
            var damage: Int = 0, var cs: Int = 0,
            var games: Int = 0,
            val champions: MutableMap<String, Int> = mutableMapOf()
        )

        val statsMap = members.associate { it.puuid to Acc(it.riotId) }.toMutableMap()

        for (match in matches) {
            for (p in match.participants) {
                val s = statsMap[p.puuid] ?: continue
                s.games++
                if (p.win) s.wins++ else s.losses++
                s.kills += p.kills; s.deaths += p.deaths; s.assists += p.assists
                s.damage += p.damage; s.cs += p.cs
                s.champions[p.champion] = (s.champions[p.champion] ?: 0) + 1
            }
        }

        val stats = statsMap.values
            .filter { it.games > 0 }
            .map { s ->
                PlayerStatsResult(
                    riotId = s.riotId,
                    games = s.games,
                    wins = s.wins,
                    losses = s.losses,
                    winRate = (s.wins * 100 / s.games),
                    avgKills = (s.kills.toDouble() / s.games * 10).toInt() / 10.0,
                    avgDeaths = (s.deaths.toDouble() / s.games * 10).toInt() / 10.0,
                    avgAssists = (s.assists.toDouble() / s.games * 10).toInt() / 10.0,
                    kda = if (s.deaths > 0) ((s.kills + s.assists).toDouble() / s.deaths * 100).toInt() / 100.0
                          else (s.kills + s.assists).toDouble(),
                    avgDamage = s.damage / s.games,
                    avgCs = (s.cs.toDouble() / s.games * 10).toInt() / 10.0,
                    topChampions = s.champions.entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { ChampionCount(it.key, it.value) }
                )
            }
            .sortedWith(compareByDescending<PlayerStatsResult> { it.winRate }.thenByDescending { it.games })

        return StatsResult(stats, matches.size.toLong())
    }
}
