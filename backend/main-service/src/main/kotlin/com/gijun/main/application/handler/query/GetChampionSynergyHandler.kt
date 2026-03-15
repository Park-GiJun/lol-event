package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionSynergy
import com.gijun.main.application.dto.stats.result.ChampionSynergyResult
import com.gijun.main.application.port.`in`.GetChampionSynergyUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetChampionSynergyHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetChampionSynergyUseCase {

    override fun getChampionSynergy(mode: String, minGames: Int): ChampionSynergyResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class Acc(
            val c1: String, val c1Id: Int,
            val c2: String, val c2Id: Int,
            var games: Int = 0, var wins: Int = 0, var kills: Int = 0,
        )

        val accMap = mutableMapOf<String, Acc>()

        for (match in matches) {
            val byTeam = match.participants.groupBy { it.teamId }
            for ((_, team) in byTeam) {
                for (i in team.indices) {
                    for (j in i + 1 until team.size) {
                        val (a, b) = if (team[i].champion <= team[j].champion)
                            team[i] to team[j] else team[j] to team[i]
                        val key = "${a.champion}|${b.champion}"
                        val acc = accMap.getOrPut(key) { Acc(a.champion, a.championId, b.champion, b.championId) }
                        acc.games++
                        if (a.win) acc.wins++
                        acc.kills += a.kills + b.kills
                    }
                }
            }
        }

        val synergies = accMap.values
            .filter { it.games >= minGames }
            .map { s ->
                ChampionSynergy(
                    champion1 = s.c1, champion1Id = s.c1Id,
                    champion2 = s.c2, champion2Id = s.c2Id,
                    games = s.games, wins = s.wins,
                    winRate = s.wins * 100 / s.games,
                    avgCombinedKills = (s.kills.toDouble() / s.games * 10).toInt() / 10.0,
                )
            }
            .sortedWith(compareByDescending<ChampionSynergy> { it.winRate }.thenByDescending { it.games })

        return ChampionSynergyResult(synergies, matches.size)
    }
}
