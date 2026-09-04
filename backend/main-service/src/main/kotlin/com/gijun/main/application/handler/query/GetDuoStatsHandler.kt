package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.DuoStat
import com.gijun.main.application.dto.stats.result.DuoStatsResult
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.service.RankingScore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetDuoStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetDuoStatsUseCase {

    override fun getDuoStats(mode: String, minGames: Int): DuoStatsResult = cache.getOrCompute("duo-stats:$mode:$minGames") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class Acc(
            val p1: String, val p2: String,
            var games: Int = 0, var wins: Int = 0,
            var kills: Int = 0, var deaths: Int = 0, var assists: Int = 0,
        )

        val accMap = mutableMapOf<String, Acc>()

        for (match in matches) {
            val byTeam = match.participants.groupBy { it.teamId }
            for ((_, team) in byTeam) {
                for (i in team.indices) {
                    for (j in i + 1 until team.size) {
                        val (a, b) = if (team[i].riotId <= team[j].riotId)
                            team[i] to team[j] else team[j] to team[i]
                        val key = "${a.riotId}|${b.riotId}"
                        val acc = accMap.getOrPut(key) { Acc(a.riotId, b.riotId) }
                        acc.games++
                        if (a.win) acc.wins++
                        acc.kills += a.kills + b.kills
                        acc.deaths += a.deaths + b.deaths
                        acc.assists += a.assists + b.assists
                    }
                }
            }
        }

        fun r1(v: Double) = (v * 10).toInt() / 10.0
        fun r2(v: Double) = (v * 100).toInt() / 100.0

        val duos = accMap.values
            .filter { it.games >= minGames }
            .map { s ->
                DuoStat(
                    player1 = s.p1, player2 = s.p2,
                    games = s.games, wins = s.wins,
                    winRate = s.wins * 100 / s.games,
                    adjustedWinRate = r2(RankingScore.shrunkWinRate(s.wins, s.games)),
                    sampleGrade = RankingScore.sampleGrade(s.games),
                    avgKills   = r1(s.kills.toDouble() / s.games),
                    avgDeaths  = r1(s.deaths.toDouble() / s.games),
                    avgAssists = r1(s.assists.toDouble() / s.games),
                    kda = if (s.deaths > 0) r2((s.kills + s.assists).toDouble() / s.deaths)
                          else (s.kills + s.assists).toDouble(),
                )
            }
            // 관측 승률로 정렬하면 5경기 5승 듀오가 90경기를 함께한 듀오를 이긴다.
            .sortedWith(compareByDescending<DuoStat> { it.adjustedWinRate }.thenByDescending { it.games })

        DuoStatsResult(duos)
    }
}
