package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.SessionEntry
import com.gijun.main.application.dto.stats.result.SessionReportResult
import com.gijun.main.application.port.`in`.GetSessionReportUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetSessionReportHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetSessionReportUseCase {

    override fun getSessionReport(mode: String): SessionReportResult = cache.getOrCompute("session-report:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        fun r2(v: Double) = (v * 100).toInt() / 100.0

        val kstZone = java.time.ZoneId.of("Asia/Seoul")

        val byDate = matches.groupBy { match ->
            java.time.Instant.ofEpochMilli(match.gameCreation)
                .atZone(kstZone).toLocalDate()
        }

        val sessions = byDate.entries
            .sortedByDescending { it.key }
            .map { (date, dayMatches) ->
                val totalDurationSec = dayMatches.sumOf { it.gameDuration }
                val totalKills = dayMatches.sumOf { m -> m.participants.sumOf { it.kills } }

                // 팀100/팀200 승수
                var team100Wins = 0
                var team200Wins = 0
                for (m in dayMatches) {
                    val winner = m.participants.firstOrNull { it.win }
                    if (winner != null) {
                        if (winner.teamId == 100) team100Wins++ else team200Wins++
                    }
                }

                // MatchParticipant.pentaKills 는 처음부터 있었다. 없다고 적힌 주석을 믿고
                // 0 을 그대로 내보내고 있어서, 실제로 나온 펜타(수집분 7회)가 세션 보고서에서
                // 전부 사라져 있었다.
                val pentaKills = dayMatches.sumOf { m -> m.participants.sumOf { it.pentaKills } }

                // 세션 MVP: 최고 KDA 플레이어
                data class PlayerKda(val riotId: String, val kda: Double)
                val playerKdas = dayMatches
                    .flatMap { it.participants }
                    .groupBy { it.riotId }
                    .map { (riotId, ps) ->
                        val k = ps.sumOf { it.kills }
                        val d = ps.sumOf { it.deaths }
                        val a = ps.sumOf { it.assists }
                        val kda = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()
                        PlayerKda(riotId, kda)
                    }
                val mvpEntry = playerKdas.maxByOrNull { it.kda }

                val participants = dayMatches
                    .flatMap { it.participants }
                    .map { it.riotId }
                    .distinct()
                    .sorted()

                SessionEntry(
                    date = date.toString(),
                    games = dayMatches.size,
                    totalDurationMin = totalDurationSec / 60,
                    sessionMvp = mvpEntry?.riotId,
                    sessionMvpKda = mvpEntry?.kda ?: 0.0,
                    team100Wins = team100Wins,
                    team200Wins = team200Wins,
                    totalKills = totalKills,
                    pentaKills = pentaKills,
                    participants = participants,
                )
            }

        SessionReportResult(
            sessions = sessions,
            totalSessions = sessions.size,
        )
    }
}
