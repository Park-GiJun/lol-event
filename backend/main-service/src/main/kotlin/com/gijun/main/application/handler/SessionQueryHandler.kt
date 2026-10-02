package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetSessionDetailQuery
import com.gijun.main.application.dto.result.SessionDetailResult
import com.gijun.main.application.dto.result.SessionEntry
import com.gijun.main.application.dto.result.SessionMatchEntry
import com.gijun.main.application.dto.result.SessionPlayerEntry
import com.gijun.main.application.dto.result.SessionReportResult
import com.gijun.main.application.dto.result.SessionStreak
import com.gijun.main.application.port.`in`.GetSessionDetailUseCase
import com.gijun.main.application.port.`in`.GetSessionReportUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.match.model.MatchTimelineModel
import com.gijun.main.domain.match.service.TeamFightDetector
import com.gijun.main.domain.match.service.TimelineMetrics
import com.gijun.main.domain.match.service.TimelineParser
import com.gijun.main.domain.session.exception.InvalidSessionDateException
import com.gijun.main.domain.session.service.SessionClock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt

/**
 * 세션(하루치 내전) 조회 — 세션 목록과 세션 상세.
 */
@Service
@Transactional(readOnly = true)
class SessionQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetSessionReportUseCase,
    GetSessionDetailUseCase {
    /**
     * 캐시 키에 `v2` 가 붙은 이유: 세션 경계가 자정에서 오전 6시로 바뀌었다
     * ([SessionClock.DAY_START_HOUR]). 키를 그대로 두면 배포 직후 5분간 옛 경계로 묶인
     * 응답이 그대로 나간다.
     */
    override fun getSessionReport(mode: GameMode): SessionReportResult =
        statsResultCacheQueryPort.getOrCompute("session-report:v2:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            val byDate = matches.groupBy { SessionClock.sessionDate(it.gameCreation) }

            val sessions =
                byDate.entries
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

                        // MatchParticipantModel.pentaKills 는 처음부터 있었다. 없다고 적힌 주석을 믿고
                        // 0 을 그대로 내보내고 있어서, 실제로 나온 펜타(수집분 7회)가 세션 보고서에서
                        // 전부 사라져 있었다.
                        val pentaKills = dayMatches.sumOf { m -> m.participants.sumOf { it.pentaKills } }

                        // 세션 MVP: 최고 KDA 플레이어
                        data class PlayerKda(
                            val riotId: String,
                            val kda: Double,
                        )
                        val playerKdas =
                            dayMatches
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

                        val participants =
                            dayMatches
                                .flatMap { it.participants }
                                .map { it.riotId }
                                .distinct()
                                .sorted()

                        SessionEntry(
                            date = date.toString(),
                            matchIds = dayMatches.sortedBy { it.gameCreation }.map { it.matchId },
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

    /**
     * 하루치 내전 상세.
     *
     * 스냅샷을 쓰지 않는다. 세션 하나는 경기 수가 유계(실측 최대 열몇 판)라 요청 시 파싱해도
     * 원본이 1MB 수준이고, 스냅샷을 만들면 "세션이 언제 바뀌나"를 관리하는 비용만 생긴다.
     * 그래서 경기 상세와 같은 실시간 파싱 + 캐시 경로를 쓴다.
     *
     * 세션 히트맵과 좌표 체류 비율은 **일부러 넣지 않았다**. 열몇 판의 프레임 표본으로는
     * [com.gijun.main.domain.match.service.PositionMetrics] 의 분당 1점 한계 때문에 숫자가 소음이다.
     */
    override fun getSessionDetail(query: GetSessionDetailQuery): SessionDetailResult? {
        val day = parse(query.date)
        return statsResultCacheQueryPort.getOrCompute("session-detail:${query.date}:${query.mode.key}") { build(day, query.mode) }
    }

    /** `yyyy-MM-dd` 가 아니면 400 이다. 404 가 아닌 이유: 날짜 자체가 잘못된 요청이다. */
    private fun parse(date: String): LocalDate =
        try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw InvalidSessionDateException(date)
        }

    private fun build(
        day: LocalDate,
        mode: GameMode,
    ): SessionDetailResult? {
        val (fromMs, untilMs) = SessionClock.rangeMs(day)
        val matches = matchQueryPersistencePort.findInPeriodWithParticipants(mode.queueIds, fromMs, untilMs)
        if (matches.isEmpty()) return null

        val raws = matchQueryPersistencePort.findTimelineRaw(matches.map { it.matchId })
        val timelines = matches.associate { it.matchId to TimelineParser.parse(raws[it.matchId]) }
        val metrics =
            matches
                .mapNotNull { m -> TimelineMetrics.of(m, timelines.getValue(m.matchId)) }
                .associateBy { it.matchId }

        val entries = matches.map { m -> matchEntry(m, timelines.getValue(m.matchId), metrics[m.matchId]) }
        val fights =
            matches.flatMap { m ->
                TeamFightDetector.of(timelines.getValue(m.matchId), teamByPid(m)).filter { it.isTeamFight }
            }

        val decided = entries.filter { it.winnerTeamId != null }
        val comeback =
            entries
                .filter { it.goldDiffAt15 != null && it.winnerTeamId != null }
                .filter { behindAt15(it) }
                .minByOrNull { blueOriented(it) }

        return SessionDetailResult(
            date = day.toString(),
            games = matches.size,
            timelineGames = entries.count { it.hasTimeline },
            firstGameAt = matches.first().gameCreation,
            lastGameAt = matches.last().gameCreation,
            totalDurationMin = matches.sumOf { it.gameDuration } / 60,
            totalKills = matches.sumOf { m -> m.participants.sumOf { it.kills } },
            pentaKills = matches.sumOf { m -> m.participants.sumOf { it.pentaKills } },
            team100Wins = decided.count { it.winnerTeamId == 100 },
            team200Wins = decided.count { it.winnerTeamId == 200 },
            matches = entries,
            players = players(matches, metrics),
            teamFights = fights.size,
            team100FightWins = fights.count { it.winnerTeamId == 100 },
            team200FightWins = fights.count { it.winnerTeamId == 200 },
            longestWinStreak = streak(matches, won = true),
            longestLossStreak = streak(matches, won = false),
            biggestComeback = comeback,
            longestGame = matches.maxByOrNull { it.gameDuration }?.let { m -> entries.first { it.matchId == m.matchId } },
            shortestGame = matches.minByOrNull { it.gameDuration }?.let { m -> entries.first { it.matchId == m.matchId } },
        )
    }

    // ────────── 경기 ──────────

    private fun matchEntry(
        match: MatchModel,
        timeline: MatchTimelineModel,
        metrics: TimelineMetrics.MatchMetrics?,
    ): SessionMatchEntry {
        val curve = teamGoldDiffByMinute(match, timeline)
        return SessionMatchEntry(
            matchId = match.matchId,
            gameCreation = match.gameCreation,
            durationSec = match.gameDuration,
            winnerTeamId = match.participants.firstOrNull { it.win }?.teamId,
            totalKills = match.participants.sumOf { it.kills },
            hasTimeline = curve.isNotEmpty(),
            teamGoldDiffByMinute = curve,
            goldDiffAt15 =
                metrics?.goldLeadTeamAt15?.let { lead ->
                    if (lead == 100) metrics.teamGoldGapAt15 else -metrics.teamGoldGapAt15
                },
        )
    }

    /**
     * 블루 − 레드 팀 골드 격차 곡선. 0분부터 끊기지 않고 이어지는 프레임만 쓴다
     * (경기 상세와 같은 규약 — 중간을 앞 값으로 채우면 없던 정체 구간이 생긴다).
     */
    private fun teamGoldDiffByMinute(
        match: MatchModel,
        timeline: MatchTimelineModel,
    ): List<Int> {
        val teamByPid = teamByPid(match)
        if (teamByPid.isEmpty()) return emptyList()

        val byMinute = timeline.frames.associateBy { (it.timestampMs / 60_000L).toInt() }
        val out = mutableListOf<Int>()
        var minute = 0
        while (true) {
            val frame = byMinute[minute] ?: break
            val blue =
                frame.participants.values
                    .filter { teamByPid[it.participantId] == 100 }
                    .sumOf { it.totalGold }
            val red =
                frame.participants.values
                    .filter { teamByPid[it.participantId] == 200 }
                    .sumOf { it.totalGold }
            out.add(blue - red)
            minute++
        }
        return out
    }

    private fun teamByPid(match: MatchModel): Map<Int, Int> =
        match.participants
            .filter { it.participantId > 0 && it.riotId.isNotBlank() }
            .associate { it.participantId to it.teamId }

    /** 15분에 뒤지고 있었는데 이긴 경기인가. */
    private fun behindAt15(entry: SessionMatchEntry): Boolean {
        val diff = entry.goldDiffAt15 ?: return false
        val winner = entry.winnerTeamId ?: return false
        return (winner == 100 && diff < 0) || (winner == 200 && diff > 0)
    }

    /** 역전 폭을 한 축으로 눕힌다 — 이긴 팀 관점의 15분 격차. 작을수록 크게 뒤졌다. */
    private fun blueOriented(entry: SessionMatchEntry): Int {
        val diff = entry.goldDiffAt15 ?: 0
        return if (entry.winnerTeamId == 200) -diff else diff
    }

    // ────────── 사람 ──────────

    private fun players(
        matches: List<MatchModel>,
        metrics: Map<String, TimelineMetrics.MatchMetrics>,
    ): List<SessionPlayerEntry> {
        val lines = metrics.values.flatMap { m -> m.players.map { it } }.groupBy { it.riotId }

        return matches
            .flatMap { it.participants }
            .filter { it.riotId.isNotBlank() }
            .groupBy { it.riotId }
            .map { (riotId, ps) ->
                val kills = ps.sumOf { it.kills }
                val deaths = ps.sumOf { it.deaths }
                val assists = ps.sumOf { it.assists }
                val mine = lines[riotId].orEmpty()
                val laneDiffs = mine.mapNotNull { it.goldDiff15 }

                SessionPlayerEntry(
                    riotId = riotId,
                    games = ps.size,
                    wins = ps.count { it.win },
                    kills = kills,
                    deaths = deaths,
                    assists = assists,
                    kda = if (deaths > 0) r2((kills + assists).toDouble() / deaths) else (kills + assists).toDouble(),
                    avgGoldDiff15 = laneDiffs.takeIf { it.isNotEmpty() }?.let { r1(it.average()) },
                    laneGames = laneDiffs.size,
                    earlyKills = mine.sumOf { it.earlyKills },
                    earlyDeaths = mine.sumOf { it.earlyDeaths },
                    soloKills = mine.sumOf { it.soloKills },
                )
            }.sortedWith(compareByDescending<SessionPlayerEntry> { it.wins }.thenByDescending { it.kda })
    }

    /**
     * 최대 연승·연패. 팀이 매 판 바뀌므로 **사람** 기준으로 센다.
     *
     * 한 세션에서 한 사람이 쭉 이겼다면 그게 그 사람의 하루다 — 팀 기준으로 세면 팀 구성이
     * 바뀔 때마다 끊겨서 의미가 없다.
     */
    private fun streak(
        matches: List<MatchModel>,
        won: Boolean,
    ): SessionStreak? {
        val best = mutableMapOf<String, Int>()
        val current = mutableMapOf<String, Int>()

        for (match in matches) {
            for (p in match.participants.filter { it.riotId.isNotBlank() }) {
                if (p.win == won) {
                    val next = (current[p.riotId] ?: 0) + 1
                    current[p.riotId] = next
                    if (next > (best[p.riotId] ?: 0)) best[p.riotId] = next
                } else {
                    current[p.riotId] = 0
                }
            }
        }

        // 2연승부터 기록으로 본다. 1은 그냥 한 판 이긴 것이다.
        return best
            .filterValues { it >= 2 }
            .maxByOrNull { it.value }
            ?.let { SessionStreak(it.key, it.value) }
    }

    private fun r1(v: Double) = (v * 10).roundToInt() / 10.0

    private fun r2(v: Double) = (v * 100).roundToInt() / 100.0
}
