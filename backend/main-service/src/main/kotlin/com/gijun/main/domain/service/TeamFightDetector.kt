package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MapPoint
import com.gijun.main.domain.model.match.MatchTimeline
import com.gijun.main.domain.model.match.TimelineEvent

/**
 * 한 번의 교전.
 *
 * 킬 수가 [TeamFightDetector.MIN_TEAMFIGHT_KILLS] 미만인 묶음도 버리지 않고 돌려준다 —
 * 솔로킬과 2인 교전은 "어디서 물렸나" 지도에 쓸모가 있다. 한타만 보고 싶으면 [isTeamFight] 로 거른다.
 */
data class TeamFight(
    val startMs: Long,
    val endMs: Long,
    val kills: List<TimelineEvent.ChampionKill>,
    /** 팀 100 이 **올린** 킬 수. 희생자의 팀으로 센다. */
    val team100Kills: Int,
    val team200Kills: Int,
    /** 킬을 더 많이 올린 팀. 동수면 null — 교환으로 끝난 교전이다. */
    val winnerTeamId: Int?,
    /** 첫 킬을 올린 팀. "누가 먼저 걸었나"에 가장 가까운 값이다. */
    val openedByTeamId: Int?,
    /** 킬러 ∪ 희생자 ∪ 어시스트. 0(포탑·미니언 막타)은 빼고 담는다. */
    val participantIds: Set<Int>,
    /** 킬 좌표의 평균. 좌표가 하나도 없으면 null. */
    val centroid: MapPoint?,
    val region: MapRegion?,
    /** 교전이 끝난 뒤 [TeamFightDetector.OBJECTIVE_LINK_MS] 안에 넘어간 오브젝트·건물. */
    val objectivesAfter: List<TimelineEvent>,
) {
    val isTeamFight: Boolean get() = kills.size >= TeamFightDetector.MIN_TEAMFIGHT_KILLS
}

/**
 * 킬 이벤트를 교전 단위로 묶는다.
 *
 * **경기 종료 스탯으로는 만들 수 없는 축이다.** 최종 KDA 는 한타에서 낸 킬과 라인에서 딴 킬을
 * 구분하지 못하고, "한타를 이겼는지"는 아예 흔적이 없다.
 *
 * ## 묶는 규칙
 *
 * 세 조건 중 하나라도 어긋나면 새 교전으로 끊는다.
 *
 * 1. 직전 킬과의 간격이 [GAP_MS] 이내 — 시간으로 이어진 연쇄인가
 * 2. 교전 시작부터 [MAX_SPAN_MS] 이내 — 끝없이 물리는 연쇄를 한 덩어리로 만들지 않는다
 * 3. 교전 중심에서 [CLUSTER_RADIUS] 이내 — 맵 양쪽에서 동시에 벌어진 1대1 을 가른다
 *
 * 3번은 **좌표가 있을 때만** 적용한다. 좌표가 없는 킬은 시간 조건만으로 붙인다 — 17판 실측에서는
 * 좌표가 빠진 킬이 없었지만, 원본이 바뀌어도 묶기가 멈추지 않게 열화 규칙을 둔다.
 *
 * ## 왜 팀 정보를 인자로 받나
 *
 * 타임라인만으로는 누가 어느 팀인지 알 수 없다. `CHAMPION_KILL` 에는 팀 필드가 없고,
 * `BUILDING_KILL.teamId` 는 부서진 건물의 팀이다. [Match][com.gijun.main.domain.model.match.Match]
 * 전체는 필요 없으니 맵만 받는다 — 테스트가 훨씬 가벼워진다.
 */
object TeamFightDetector {

    /** 이 간격 안에 이어지는 킬은 같은 교전이다. 리그의 어시스트 인정 창이 10초다. */
    const val GAP_MS = 10_000L

    /**
     * 한 교전의 최대 길이. 이걸 안 걸면 끊기지 않고 계속 물리는 연쇄가 3분짜리 "한타" 하나로
     * 합쳐져, 한타 승패가 사실상 경기 승패가 된다.
     */
    const val MAX_SPAN_MS = 30_000L

    /**
     * 교전 중심에서 이 거리를 넘으면 다른 교전이다. 맵 단위(협곡 한 변이 약 14,700)로,
     * 라인 회랑 반폭([MapGeometry.LANE_HALF_WIDTH] = 1,400)의 두 배쯤 된다 —
     * 한 교전이 번지는 범위는 라인 폭보다 넓지만 맵 절반에는 못 미친다.
     */
    const val CLUSTER_RADIUS = 2_500.0

    /** 이 킬 수 이상을 한타로 부른다. 그 아래는 픽이나 트레이드다. */
    const val MIN_TEAMFIGHT_KILLS = 3

    /** 교전이 끝난 뒤 이 시간 안에 넘어간 오브젝트는 그 교전의 전리품으로 본다. */
    const val OBJECTIVE_LINK_MS = 30_000L

    fun of(timeline: MatchTimeline, teamByParticipantId: Map<Int, Int>): List<TeamFight> {
        val kills = timeline.events.filterIsInstance<TimelineEvent.ChampionKill>()
        if (kills.isEmpty()) return emptyList()

        val objectives = timeline.events.filter {
            it is TimelineEvent.EliteMonsterKill || it is TimelineEvent.BuildingKill
        }

        return cluster(kills).map { group -> fight(group, teamByParticipantId, objectives) }
    }

    // ────────── 묶기 ──────────

    private fun cluster(kills: List<TimelineEvent.ChampionKill>): List<List<TimelineEvent.ChampionKill>> {
        val groups = mutableListOf<MutableList<TimelineEvent.ChampionKill>>()
        var current = mutableListOf(kills.first())

        for (kill in kills.drop(1)) {
            if (continues(current, kill)) current.add(kill)
            else {
                groups.add(current)
                current = mutableListOf(kill)
            }
        }
        groups.add(current)
        return groups
    }

    private fun continues(group: List<TimelineEvent.ChampionKill>, kill: TimelineEvent.ChampionKill): Boolean {
        if (kill.timestampMs - group.last().timestampMs > GAP_MS) return false
        if (kill.timestampMs - group.first().timestampMs > MAX_SPAN_MS) return false

        // 좌표가 없으면 공간 조건은 건너뛴다 — 시간으로만 판단한다.
        val center = centroid(group) ?: return true
        val at = kill.position ?: return true
        return MapGeometry.distance(at, center) <= CLUSTER_RADIUS
    }

    private fun centroid(kills: List<TimelineEvent.ChampionKill>): MapPoint? {
        val points = kills.mapNotNull { it.position }
        if (points.isEmpty()) return null
        return MapPoint(points.sumOf { it.x } / points.size, points.sumOf { it.y } / points.size)
    }

    // ────────── 한 교전 ──────────

    private fun fight(
        kills: List<TimelineEvent.ChampionKill>,
        teamByParticipantId: Map<Int, Int>,
        objectives: List<TimelineEvent>,
    ): TeamFight {
        val startMs = kills.first().timestampMs
        val endMs = kills.last().timestampMs

        // 킬러가 아니라 **희생자**의 팀으로 센다. 포탑이 막타를 치면 killerId 가 0 이라 킬러로는 못 센다.
        val byScoringTeam = kills.groupingBy { other(teamByParticipantId[it.victimId]) }.eachCount()
        val team100 = byScoringTeam[100] ?: 0
        val team200 = byScoringTeam[200] ?: 0

        val center = centroid(kills)

        return TeamFight(
            startMs = startMs,
            endMs = endMs,
            kills = kills,
            team100Kills = team100,
            team200Kills = team200,
            winnerTeamId = when {
                team100 > team200 -> 100
                team200 > team100 -> 200
                else -> null
            },
            openedByTeamId = other(teamByParticipantId[kills.first().victimId]),
            participantIds = kills.flatMap { listOf(it.killerId, it.victimId) + it.assistIds }
                .filter { it > 0 }.toSet(),
            centroid = center,
            region = center?.let(MapGeometry::regionOf),
            objectivesAfter = objectives.filter { it.timestampMs in endMs..(endMs + OBJECTIVE_LINK_MS) },
        )
    }

    /** 상대 팀. 희생자의 팀을 킬을 올린 팀으로 뒤집는 데 쓴다. */
    private fun other(teamId: Int?): Int? = when (teamId) {
        100 -> 200
        200 -> 100
        else -> null
    }
}
