package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.PositionChampEntry
import com.gijun.main.application.dto.stats.result.PlayerPositionEntry
import com.gijun.main.application.dto.stats.result.PositionChampionPoolResult
import com.gijun.main.application.port.`in`.GetPositionChampionPoolUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetPositionChampionPoolHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetPositionChampionPoolUseCase {

    override fun getPositionChampionPool(mode: String): PositionChampionPoolResult = cache.getOrCompute("position-champion-pool:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class ChampAcc(var games: Int = 0, var wins: Int = 0, var kills: Int = 0, var deaths: Int = 0, var assists: Int = 0, var championId: Int = 0)

        // key: riotId + "|" + position
        val posAccMap = mutableMapOf<String, MutableMap<String, ChampAcc>>()  // riotId|position -> champion -> acc

        for (match in matches) {
            for (p in match.participants) {
                val pos = p.lane?.uppercase() ?: "UNKNOWN"
                val key = "${p.riotId}|$pos"
                val champMap = posAccMap.getOrPut(key) { mutableMapOf() }
                val acc = champMap.getOrPut(p.champion) { ChampAcc().also { it.championId = p.championId } }
                acc.games++
                acc.championId = p.championId
                if (p.win) acc.wins++
                acc.kills += p.kills
                acc.deaths += p.deaths
                acc.assists += p.assists
            }
        }

        val allPlayers = posAccMap.entries
            .filter { it.value.values.sumOf { a -> a.games } >= 3 }
            .map { (key, champAcc) ->
                val (riotId, position) = key.split("|", limit = 2)
                val totalGames = champAcc.values.sumOf { it.games }
                val totalWins = champAcc.values.sumOf { it.wins }

                val champions = champAcc.entries.map { (champ, acc) ->
                    val kda = if (acc.deaths == 0) (acc.kills + acc.assists).toDouble()
                              else (acc.kills + acc.assists).toDouble() / acc.deaths
                    PositionChampEntry(
                        champion = champ,
                        championId = acc.championId,
                        games = acc.games,
                        winRate = if (acc.games > 0) (acc.wins.toDouble() / acc.games * 1000).toInt() / 10.0 else 0.0,
                        kda = (kda * 100).toInt() / 100.0,
                    )
                }.sortedByDescending { it.games }

                val top = champions.firstOrNull()

                PlayerPositionEntry(
                    riotId = riotId,
                    position = position,
                    games = totalGames,
                    winRate = if (totalGames > 0) (totalWins.toDouble() / totalGames * 1000).toInt() / 10.0 else 0.0,
                    topChampion = top?.champion,
                    topChampionId = top?.championId,
                    champions = champions.take(10),
                )
            }
            .sortedWith(compareBy({ it.riotId }, { it.position }))

        PositionChampionPoolResult(allPlayers = allPlayers)
    }
}
