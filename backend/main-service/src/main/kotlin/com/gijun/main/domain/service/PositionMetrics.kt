package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MapPoint
import com.gijun.main.domain.model.match.MatchTimeline
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.model.match.TimelineEvent
import kotlin.math.roundToInt

/**
 * 좌표에서 나오는 경기 단위 지표. 기하 판정은 [MapGeometry] 가, 무엇을 지표라고 부를지는 여기가 정한다.
 * ([LaneScores] 가 판정이고 [TimelineMetrics] 가 지표인 것과 같은 갈라짐이다.)
 *
 * ## 프레임 기반 비율의 한계 — 이 파일 전체에 해당한다
 *
 * 프레임은 **분당 1점**이다. 40분 경기가 사람당 41점이다. 그래서 "라인 이탈 비율"은 실제로 이탈한
 * 시간의 비율이 아니라 **분 경계에 어디 있었는지의 비율**이다. 30초짜리 갱킹은 통째로 사라지거나
 * 1분(전체의 2.5%)으로 과대 집계된다. 그래서:
 *
 * 1. 한 판 값을 화면에 그대로 내보내지 않는다. 여러 판을 합친 비율만 쓴다.
 * 2. 모든 비율에 분모([PlayerPosition.framesSampled])를 같이 싣는다.
 * 3. 죽어 있는 프레임과 살아 있는 프레임을 구분할 수 없다. 프레임에는 생사 정보가 없다.
 *
 * **반면 이벤트 좌표는 정확하다** — 초 단위 타임스탬프에 실제 좌표가 붙어 온다. 그래서
 * [PlayerPosition.deathRegions] 같은 이벤트 기반 지표는 신뢰도가 높고 프레임 체류 비율은 낮다.
 * 화면에서 이 두 계열을 같은 표에 섞지 마라.
 */
object PositionMetrics {
    /** 라인전 구간의 끝. [LaneScores] 의 15분 판정과 같은 경계를 쓴다. */
    const val LANE_PHASE_MS = 900_000L

    /**
     * 0분 프레임은 전부 버린다.
     *
     * 경기 시작 직후라 열 명이 모두 자기 분수에 서 있다. 분모에 넣으면 전원이 "기지 체류"로
     * 한 점씩 세지고, 라인 점유율이 경기 길이에 따라 흔들린다.
     */
    private const val FIRST_COUNTED_MINUTE = 1

    /** 어느 팀에서 어느 자리를 맡았나. 타임라인만으로는 알 수 없어 밖에서 받는다. */
    data class Slot(
        val teamId: Int,
        val position: Position,
    )

    data class PlayerPosition(
        val participantId: Int,
        /** 좌표가 있는 프레임 수(0분 제외). 모든 비율의 분모다. */
        val framesSampled: Int,
        /** 그중 라인전 구간(1~15분) 프레임 수. 라인 관련 비율의 분모다. */
        val lanePhaseFrames: Int,
        /**
         * 라인전 동안 자기 라인 회랑 안에 있던 비율(0~100).
         * 정글·포지션 미상은 **null** 이다 — 라인이 없는 자리에 0을 매기면 "라인을 안 선다"로 읽힌다.
         */
        val laneShareRate: Double?,
        /** 라인전 동안 자기 라인도 자기 기지도 아닌 곳에 있던 비율(0~100). 정글은 null. */
        val roamRate: Double?,
        /** 상대 진영에 있던 비율(0~100). 경기 전체. */
        val enemyHalfRate: Double,
        /** 상대 진영에 있던 비율(0~100). 라인전 구간만. */
        val enemyHalfRateLanePhase: Double,
        /** 상대 진영 **정글**에 있던 비율(0~100). 카운터 정글의 대리 지표다. */
        val counterJungleRate: Double,
        /** 죽은 자리의 분포. 이벤트 좌표라 신뢰도가 높다. */
        val deathRegions: Map<MapRegion, Int>,
        /**
         * 킬을 올린 자리의 분포.
         *
         * 주의: 이벤트 좌표는 **희생자가 죽은 자리**다. 근접 교전이면 킬러 위치와 같지만,
         * 원거리로 마무리하면 다르다. "어디서 킬이 났나"로 읽어야 하고 "킬러가 어디 있었나"가 아니다.
         */
        val killRegions: Map<MapRegion, Int>,
    )

    /**
     * 팀이 얼마나 뭉쳐 다녔나. 값이 작을수록 붙어 있다. 단위는 맵 거리다.
     *
     * **자기 기지 안의 프레임은 뺀다** — 죽어서 분수에 있는 사람 하나가 팀 응집도를 통째로
     * 망가뜨린다. 부작용으로 정상적인 귀환도 같이 빠지는데, 프레임만으로는 생사를 구분할 수
     * 없어서 이게 최선이다. 부활 시간을 추정하는 건 창작이다. **알려진 편향으로 두고 쓴다.**
     */
    data class TeamSpread(
        val teamId: Int,
        /** 분 -> 그 분의 평균 거리. 셀 수 있는 사람이 2명 미만인 분은 빠진다. */
        val byMinute: Map<Int, Int>,
        val average: Int?,
    )

    data class MatchPositions(
        val players: List<PlayerPosition>,
        val teamSpreads: List<TeamSpread>,
    ) {
        fun of(participantId: Int): PlayerPosition? = players.firstOrNull { it.participantId == participantId }
    }

    /** 좌표가 하나도 없으면 null — 옛 수집기로 받은 경기가 여기로 떨어진다. */
    fun of(
        timeline: MatchTimeline,
        roster: Map<Int, Slot>,
    ): MatchPositions? {
        val samples = samples(timeline)
        if (samples.isEmpty()) return null

        val kills = timeline.events.filterIsInstance<TimelineEvent.ChampionKill>()

        return MatchPositions(
            players = roster.keys.sorted().map { pid -> player(pid, roster.getValue(pid), samples, kills) },
            teamSpreads = listOf(100, 200).map { teamId -> spread(teamId, samples, roster) },
        )
    }

    // ────────── 표본 ──────────

    /** 한 사람의 한 프레임. 좌표가 없는 프레임과 0분은 애초에 담지 않는다. */
    private class Sample(
        val minute: Int,
        val participantId: Int,
        val at: MapPoint,
    )

    /**
     * 분은 `timestamp / 60,000` 으로 구한다.
     *
     * LCU 프레임 타임스탬프는 900,330ms 처럼 정각을 조금 넘겨 오지만(분당 22ms 남짓 밀린다),
     * 40분 경기에서도 오차가 1초 미만이라 정수 나눗셈으로 충분하다.
     */
    private fun samples(timeline: MatchTimeline): List<Sample> =
        timeline.frames.flatMap { frame ->
            val minute = (frame.timestampMs / 60_000L).toInt()
            if (minute < FIRST_COUNTED_MINUTE) {
                emptyList()
            } else {
                frame.participants.values.mapNotNull { pf ->
                    pf.position?.let { Sample(minute, pf.participantId, it) }
                }
            }
        }

    // ────────── 사람 ──────────

    private fun player(
        participantId: Int,
        slot: Slot,
        samples: List<Sample>,
        kills: List<TimelineEvent.ChampionKill>,
    ): PlayerPosition {
        val mine = samples.filter { it.participantId == participantId }
        val lanePhase = mine.filter { it.minute * 60_000L <= LANE_PHASE_MS }
        val lane = MapGeometry.laneOf(slot.position)
        val ownBase = if (slot.teamId == 100) MapRegion.BLUE_BASE else MapRegion.RED_BASE

        val laneRegion =
            when (lane) {
                Lane.TOP -> MapRegion.TOP_LANE
                Lane.MID -> MapRegion.MID_LANE
                Lane.BOT -> MapRegion.BOT_LANE
                null -> null
            }

        return PlayerPosition(
            participantId = participantId,
            framesSampled = mine.size,
            lanePhaseFrames = lanePhase.size,
            laneShareRate = laneRegion?.let { rate(lanePhase) { MapGeometry.regionOf(it.at) == laneRegion } },
            roamRate =
                laneRegion?.let {
                    rate(lanePhase) { MapGeometry.regionOf(it.at).let { r -> r != laneRegion && r != ownBase } }
                },
            enemyHalfRate = rate(mine) { MapGeometry.halfOf(it.at) != slot.teamId } ?: 0.0,
            enemyHalfRateLanePhase = rate(lanePhase) { MapGeometry.halfOf(it.at) != slot.teamId } ?: 0.0,
            counterJungleRate =
                rate(mine) {
                    MapGeometry.halfOf(it.at) != slot.teamId && MapGeometry.isJungle(MapGeometry.regionOf(it.at))
                } ?: 0.0,
            deathRegions = regions(kills.filter { it.victimId == participantId }),
            killRegions = regions(kills.filter { it.killerId == participantId }),
        )
    }

    private fun rate(
        samples: List<Sample>,
        predicate: (Sample) -> Boolean,
    ): Double? = samples.takeIf { it.isNotEmpty() }?.let { r1(it.count(predicate) * 100.0 / it.size) }

    private fun regions(kills: List<TimelineEvent.ChampionKill>): Map<MapRegion, Int> =
        kills
            .mapNotNull { it.position }
            .groupingBy(MapGeometry::regionOf)
            .eachCount()

    // ────────── 팀 ──────────

    private fun spread(
        teamId: Int,
        samples: List<Sample>,
        roster: Map<Int, Slot>,
    ): TeamSpread {
        val ownBase = if (teamId == 100) MapRegion.BLUE_BASE else MapRegion.RED_BASE
        val mine =
            samples
                .filter { roster[it.participantId]?.teamId == teamId }
                .filterNot { MapGeometry.regionOf(it.at) == ownBase }

        val byMinute =
            mine
                .groupBy { it.minute }
                .mapNotNull { (minute, ss) ->
                    val points = ss.map { it.at }
                    if (points.size < 2) null else minute to spreadOf(points)
                }.toMap()

        return TeamSpread(
            teamId = teamId,
            byMinute = byMinute,
            average =
                byMinute.values
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.roundToInt(),
        )
    }

    /** 중심으로부터의 평균 거리. */
    private fun spreadOf(points: List<MapPoint>): Int {
        val center = MapPoint(points.sumOf { it.x } / points.size, points.sumOf { it.y } / points.size)
        return points.map { MapGeometry.distance(it, center) }.average().roundToInt()
    }

    private fun r1(v: Double) = (v * 10).roundToInt() / 10.0
}
