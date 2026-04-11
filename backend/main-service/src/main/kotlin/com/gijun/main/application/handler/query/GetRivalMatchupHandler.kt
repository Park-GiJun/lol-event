package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.RivalMatchupEntry
import com.gijun.main.application.dto.stats.result.RivalMatchupResult
import com.gijun.main.application.port.`in`.GetRivalMatchupUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetRivalMatchupHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetRivalMatchupUseCase {
    override fun getRivalMatchups(mode: String, minGames: Int): RivalMatchupResult = cache.getOrCompute("rival-matchup:$mode:$minGames") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class RivalRecord(var games: Int = 0, var firstWins: Int = 0, var secondWins: Int = 0)

        val recordMap = mutableMapOf<String, RivalRecord>()
        // key format: "playerA|playerB" where playerA <= playerB alphabetically
        // firstWins = wins for playerA, secondWins = wins for playerB

        for (match in matches) {
            val team1Players = match.participants.filter { it.teamId == 100 }
            val team2Players = match.participants.filter { it.teamId == 200 }
            val team1Won = team1Players.any { it.win }

            for (p1 in team1Players) {
                for (p2 in team2Players) {
                    val (a, b) = if (p1.riotId <= p2.riotId) p1 to p2 else p2 to p1
                    val key = "${a.riotId}|${b.riotId}"
                    val record = recordMap.getOrPut(key) { RivalRecord() }
                    record.games++
                    // a is from team1 if p1.riotId <= p2.riotId, else a is from team2
                    val aIsTeam1 = p1.riotId <= p2.riotId
                    if (aIsTeam1) {
                        if (team1Won) record.firstWins++ else record.secondWins++
                    } else {
                        if (team1Won) record.secondWins++ else record.firstWins++
                    }
                }
            }
        }

        val rivalries = recordMap.entries
            .filter { it.value.games >= minGames }
            .map { (key, record) ->
                val (player1, player2) = key.split("|")
                val winRate = if (record.games > 0) record.firstWins * 100 / record.games else 0
                RivalMatchupEntry(
                    player1 = player1,
                    player2 = player2,
                    games = record.games,
                    player1Wins = record.firstWins,
                    player2Wins = record.secondWins,
                    player1WinRate = winRate,
                )
            }
            .sortedByDescending { it.games }

        RivalMatchupResult(
            rivalries = rivalries,
            topRivalry = rivalries.firstOrNull(),
        )
    }
}
