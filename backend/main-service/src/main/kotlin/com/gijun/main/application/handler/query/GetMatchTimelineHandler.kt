package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.match.result.KillEntry
import com.gijun.main.application.dto.match.result.MapPointDto
import com.gijun.main.application.dto.match.result.MatchTimelineResult
import com.gijun.main.application.dto.match.result.ObjectiveEntry
import com.gijun.main.application.dto.match.result.ParticipantTimelineSeries
import com.gijun.main.application.dto.match.result.TeamFightEntry
import com.gijun.main.application.dto.match.result.TeamTimelineSeries
import com.gijun.main.application.port.`in`.GetMatchTimelineUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.model.match.MapPoint
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.MatchTimeline
import com.gijun.main.domain.model.match.ParticipantFrame
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.model.match.TimelineEvent
import com.gijun.main.domain.model.match.TimelineFrame
import com.gijun.main.domain.service.MapGeometry
import com.gijun.main.domain.service.PositionMetrics
import com.gijun.main.domain.service.TeamFight
import com.gijun.main.domain.service.TeamFightDetector
import com.gijun.main.domain.service.TimelineMetrics
import com.gijun.main.domain.service.TimelineParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 경기 한 판의 타임라인.
 *
 * 전체 통계 경로([GetTimelineStatsHandler])와 **완전히 분리된 단건 경로**다. 한 경기의 원본만
 * 읽으므로 모드 개념이 없고, 전체 스캔의 힙 문제와도 무관하다.
 *
 * 경기 존재 확인은 캐시 밖에서 한다 — 캐시에 null 을 담지 않기 위해서다. 비싼 건 60KB 원본
 * 파싱이고 PK 조회는 그에 비해 싸다.
 */
@Service
@Transactional(readOnly = true)
class GetMatchTimelineHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetMatchTimelineUseCase {
    override fun getMatchTimeline(matchId: String): MatchTimelineResult? {
        val match = matchPersistencePort.findByMatchId(matchId) ?: return null
        return cache.getOrCompute("match-timeline:$matchId") {
            build(match, matchPersistencePort.findTimelineRaw(listOf(matchId))[matchId])
        }
    }

    private fun build(
        match: Match,
        raw: String?,
    ): MatchTimelineResult {
        val durationMs = match.gameDuration * 1_000L
        val timeline = TimelineParser.parse(raw)

        val named = match.participants.filter { it.riotId.isNotBlank() && it.participantId > 0 }
        val byPid = named.associateBy { it.participantId }
        val frames = consecutiveFrames(timeline)

        // participantId 가 0 인 옛 경기는 프레임과 사람을 이을 수 없다 — 타임라인이 없는 것과 같다.
        if (frames.isEmpty() || byPid.isEmpty()) {
            return MatchTimelineResult(
                matchId = match.matchId,
                hasTimeline = false,
                durationMs = durationMs,
                lastMinute = 0,
                teams = emptyList(),
                teamGoldDiffByMinute = emptyList(),
                participants = emptyList(),
                kills = emptyList(),
                objectives = emptyList(),
                teamFights = emptyList(),
            )
        }

        val teamByPid = byPid.mapValues { it.value.teamId }
        val roster = byPid.mapValues { PositionMetrics.Slot(it.value.teamId, positionOf(it.value.assignedPosition)) }

        val lines =
            TimelineMetrics
                .of(match, timeline)
                ?.players
                ?.associateBy { it.participantId }
                .orEmpty()
        val positions = PositionMetrics.of(timeline, roster)
        val spreads = positions?.teamSpreads?.associateBy { it.teamId }.orEmpty()

        val teamGold =
            listOf(100, 200).associateWith { teamId ->
                frames.map { f -> sumOf(f, byPid, teamId) { it.totalGold } }
            }

        return MatchTimelineResult(
            matchId = match.matchId,
            hasTimeline = true,
            durationMs = durationMs,
            lastMinute = frames.size - 1,
            teams =
                listOf(100, 200).map { teamId ->
                    TeamTimelineSeries(
                        teamId = teamId,
                        win = named.firstOrNull { it.teamId == teamId }?.win ?: false,
                        goldByMinute = teamGold.getValue(teamId),
                        xpByMinute = frames.map { f -> sumOf(f, byPid, teamId) { it.xp } },
                        csByMinute = frames.map { f -> sumOf(f, byPid, teamId) { it.cs } },
                        spreadByMinute = frames.indices.map { spreads[teamId]?.byMinute?.get(it) },
                        avgSpread = spreads[teamId]?.average,
                    )
                },
            teamGoldDiffByMinute = frames.indices.map { teamGold.getValue(100)[it] - teamGold.getValue(200)[it] },
            participants =
                byPid.keys.sorted().map { pid ->
                    series(pid, byPid.getValue(pid), frames, lines[pid], positions?.of(pid))
                },
            kills = timeline.events.filterIsInstance<TimelineEvent.ChampionKill>().map { kill(it, teamByPid) },
            objectives = timeline.events.mapNotNull { objective(it, teamByPid) },
            teamFights = TeamFightDetector.of(timeline, teamByPid).map(::fight),
        )
    }

    // ────────── 프레임 ──────────

    /**
     * 0분부터 **끊기지 않고 이어지는** 프레임만 쓴다. 중간에 빠진 분이 있으면 거기서 자른다.
     *
     * 분 단위 목록이 index = 분이라는 규약을 지키려면 중간에 구멍이 없어야 한다. 앞 값을
     * 끌어다 채우면 곡선이 있지도 않은 정체 구간을 만들어 낸다. [TimelineMetrics] 의
     * 골드 격차 곡선도 같은 자리에서 끊는다.
     */
    private fun consecutiveFrames(timeline: MatchTimeline): List<TimelineFrame> {
        val byMinute = timeline.frames.associateBy { (it.timestampMs / 60_000L).toInt() }
        val out = mutableListOf<TimelineFrame>()
        var minute = 0
        while (true) {
            out.add(byMinute[minute] ?: break)
            minute++
        }
        return out
    }

    private fun sumOf(
        frame: TimelineFrame,
        byPid: Map<Int, MatchParticipant>,
        teamId: Int,
        pick: (ParticipantFrame) -> Int,
    ): Int =
        frame.participants.values
            .filter { byPid[it.participantId]?.teamId == teamId }
            .sumOf(pick)

    private fun series(
        pid: Int,
        participant: MatchParticipant,
        frames: List<TimelineFrame>,
        line: TimelineMetrics.PlayerLine?,
        position: PositionMetrics.PlayerPosition?,
    ) = ParticipantTimelineSeries(
        participantId = pid,
        riotId = participant.riotId,
        champion = participant.champion,
        championId = participant.championId,
        teamId = participant.teamId,
        position = participant.assignedPosition,
        win = participant.win,
        goldByMinute = frames.map { it.participants[pid]?.totalGold ?: 0 },
        xpByMinute = frames.map { it.participants[pid]?.xp ?: 0 },
        csByMinute = frames.map { it.participants[pid]?.cs ?: 0 },
        levelByMinute = frames.map { it.participants[pid]?.level ?: 0 },
        currentGoldByMinute = frames.map { it.participants[pid]?.currentGold ?: 0 },
        goldDiffByMinute = line?.goldDiffByMinute.orEmpty(),
        positionsByMinute = frames.map { it.participants[pid]?.position?.let(::point) },
        laneShareRate = position?.laneShareRate,
        roamRate = position?.roamRate,
        enemyHalfRate = position?.enemyHalfRate ?: 0.0,
        counterJungleRate = position?.counterJungleRate ?: 0.0,
        framesSampled = position?.framesSampled ?: 0,
        goldDiff15 = line?.goldDiff15,
        csDiff15 = line?.csDiff15,
        xpDiff15 = line?.xpDiff15,
        earlyKills = line?.earlyKills ?: 0,
        earlyDeaths = line?.earlyDeaths ?: 0,
        earlyAssists = line?.earlyAssists ?: 0,
        soloKills = line?.soloKills ?: 0,
        firstDeathMs = line?.firstDeathMs,
    )

    // ────────── 이벤트 ──────────

    private fun kill(
        event: TimelineEvent.ChampionKill,
        teamByPid: Map<Int, Int>,
    ) = KillEntry(
        timestampMs = event.timestampMs,
        minute = minute(event.timestampMs),
        killerParticipantId = event.killerId,
        victimParticipantId = event.victimId,
        assistParticipantIds = event.assistIds,
        // 킬러가 아니라 희생자의 팀을 뒤집는다 — 막타가 포탑이면 killerId 가 0 이다.
        killingTeamId = opposing(teamByPid[event.victimId]),
        at = event.position?.let(::point),
        region = event.position?.let { MapGeometry.regionOf(it).name },
    )

    private fun objective(
        event: TimelineEvent,
        teamByPid: Map<Int, Int>,
    ): ObjectiveEntry? =
        when (event) {
            is TimelineEvent.EliteMonsterKill ->
                ObjectiveEntry(
                    timestampMs = event.timestampMs,
                    minute = minute(event.timestampMs),
                    kind = event.monsterType,
                    subType = event.monsterSubType,
                    lane = "",
                    towerType = "",
                    killingTeamId = teamByPid[event.killerId],
                    killerParticipantId = event.killerId,
                    assistParticipantIds = event.assistIds,
                    at = event.position?.let(::point),
                )
            is TimelineEvent.BuildingKill ->
                ObjectiveEntry(
                    timestampMs = event.timestampMs,
                    minute = minute(event.timestampMs),
                    kind = event.buildingType,
                    subType = "",
                    lane = event.laneType,
                    towerType = event.towerType,
                    // 원본의 teamId 는 **부서진** 건물의 팀이다. 여기서 한 번만 뒤집는다.
                    killingTeamId = opposing(event.buildingTeamId),
                    killerParticipantId = event.killerId,
                    assistParticipantIds = event.assistIds,
                    at = event.position?.let(::point),
                )
            is TimelineEvent.ChampionKill -> null
        }

    private fun fight(fight: TeamFight) =
        TeamFightEntry(
            startMs = fight.startMs,
            endMs = fight.endMs,
            startMinute = minute(fight.startMs),
            team100Kills = fight.team100Kills,
            team200Kills = fight.team200Kills,
            winnerTeamId = fight.winnerTeamId,
            openedByTeamId = fight.openedByTeamId,
            participantIds = fight.participantIds.sorted(),
            at = fight.centroid?.let(::point),
            region = fight.region?.name,
            isTeamFight = fight.isTeamFight,
            objectiveKinds =
                fight.objectivesAfter.map {
                    when (it) {
                        is TimelineEvent.EliteMonsterKill -> it.monsterType
                        is TimelineEvent.BuildingKill -> it.buildingType
                        is TimelineEvent.ChampionKill -> "CHAMPION_KILL"
                    }
                },
        )

    // ────────── 잡동사니 ──────────

    private fun point(p: MapPoint) = MapPointDto(p.x, p.y)

    private fun minute(timestampMs: Long) = (timestampMs / 60_000L).toInt()

    private fun opposing(teamId: Int?): Int? =
        when (teamId) {
            100 -> 200
            200 -> 100
            else -> null
        }

    /** 포지션 추정이 실패했거나 옛 데이터라 값이 비었으면 UNKNOWN 으로 본다. */
    private fun positionOf(name: String): Position = Position.entries.firstOrNull { it.name == name.uppercase() } ?: Position.UNKNOWN
}
