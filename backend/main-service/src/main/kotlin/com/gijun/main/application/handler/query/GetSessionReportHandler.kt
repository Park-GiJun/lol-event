package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.SessionEntry
import com.gijun.main.application.dto.stats.result.SessionReportResult
import com.gijun.main.application.port.`in`.GetSessionReportUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetSessionReportHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
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

                // 펜타킬: kills >= 5인 단일 플레이어 (근사치)
                // MatchParticipant에 pentaKills 필드가 없으므로 kills 기반으로 집계하지 않고 0으로 유지
                // (실제 pentaKills 필드 있을 경우 교체 가능)
                val pentaKills = 0

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
