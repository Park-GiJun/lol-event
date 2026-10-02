package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetDuoStatsQuery
import com.gijun.main.application.dto.query.GetRivalMatchupQuery
import com.gijun.main.application.dto.result.DuoStat
import com.gijun.main.application.dto.result.DuoStatsResult
import com.gijun.main.application.dto.result.RivalMatchupEntry
import com.gijun.main.application.dto.result.RivalMatchupResult
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.`in`.GetRivalMatchupUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.stats.service.RankingScore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사람 사이의 관계 통계 — 같은 팀이었을 때(듀오)와 상대였을 때(라이벌).
 */
@Service
@Transactional(readOnly = true)
class TeamStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetDuoStatsUseCase,
    GetRivalMatchupUseCase {
    override fun getDuoStats(query: GetDuoStatsQuery): DuoStatsResult =
        statsResultCacheQueryPort.getOrCompute("duo-stats:${query.mode.key}:${query.minGames}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            data class Acc(
                val p1: String,
                val p2: String,
                var games: Int = 0,
                var wins: Int = 0,
                var kills: Int = 0,
                var deaths: Int = 0,
                var assists: Int = 0,
            )

            val accMap = mutableMapOf<String, Acc>()

            for (match in matches) {
                val byTeam = match.participants.groupBy { it.teamId }
                for ((_, team) in byTeam) {
                    for (i in team.indices) {
                        for (j in i + 1 until team.size) {
                            val (a, b) =
                                if (team[i].riotId <= team[j].riotId) {
                                    team[i] to team[j]
                                } else {
                                    team[j] to team[i]
                                }
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

            val duos =
                accMap.values
                    .filter { it.games >= query.minGames }
                    .map { s ->
                        DuoStat(
                            player1 = s.p1,
                            player2 = s.p2,
                            games = s.games,
                            wins = s.wins,
                            winRate = s.wins * 100 / s.games,
                            adjustedWinRate = r2(RankingScore.shrunkWinRate(s.wins, s.games)),
                            sampleGrade = RankingScore.sampleGrade(s.games),
                            avgKills = r1(s.kills.toDouble() / s.games),
                            avgDeaths = r1(s.deaths.toDouble() / s.games),
                            avgAssists = r1(s.assists.toDouble() / s.games),
                            kda =
                                if (s.deaths > 0) {
                                    r2((s.kills + s.assists).toDouble() / s.deaths)
                                } else {
                                    (s.kills + s.assists).toDouble()
                                },
                        )
                    }
                    // 관측 승률로 정렬하면 5경기 5승 듀오가 90경기를 함께한 듀오를 이긴다.
                    .sortedWith(compareByDescending<DuoStat> { it.adjustedWinRate }.thenByDescending { it.games })

            DuoStatsResult(duos)
        }

    override fun getRivalMatchup(query: GetRivalMatchupQuery): RivalMatchupResult =
        statsResultCacheQueryPort.getOrCompute("rival-matchup:${query.mode.key}:${query.minGames}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            data class RivalRecord(
                var games: Int = 0,
                var firstWins: Int = 0,
                var secondWins: Int = 0,
            )

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

            val rivalries =
                recordMap.entries
                    .filter { it.value.games >= query.minGames }
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
                            player1AdjustedWinRate =
                                (RankingScore.shrunkWinRate(record.firstWins, record.games) * 100).toInt() / 100.0,
                            sampleGrade = RankingScore.sampleGrade(record.games),
                        )
                    }.sortedByDescending { it.games }

            RivalMatchupResult(
                rivalries = rivalries,
                topRivalry = rivalries.firstOrNull(),
            )
        }
}
