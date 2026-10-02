package com.gijun.main.application.dto.result

/**
 * 경기 한 판의 타임라인. 경기 상세 화면이 한 번 부른다.
 *
 * ## 단위 규약
 * - 시간은 ms. `...ByMinute` 목록은 **index = 분**이다 (`TimelineMetrics.goldDiffByMinute` 관례).
 * - 좌표는 0~14,870 (협곡 미니맵 스케일).
 * - 비율은 0~100, 소수 첫째 자리.
 *
 * ## 크기
 * 40분 10명이면 분 단위 목록이 사람당 다섯 줄 + 좌표 40점이라 **100KB 급**이다. 상세 화면
 * 한 번 호출용이고 목록 응답에는 싣지 않는다.
 */
data class MatchTimelineResult(
    val matchId: String,
    /**
     * 타임라인이 있는 경기인가.
     *
     * false 면 아래 목록이 전부 비어 있다. **404 를 주지 않는 이유**: 타임라인은 새 수집기로
     * 받은 경기에만 있고 그 이전 경기는 **영구히** 없다(LCU 히스토리에 타임라인이 없어 백필이
     * 불가능하다). 404 로 답하면 대부분의 경기 상세 화면이 에러로 뜬다. 경기 자체가 없을 때만 404 다.
     */
    val hasTimeline: Boolean,
    val durationMs: Long,
    /** 프레임이 있는 마지막 분. 곡선의 x축 끝이다. */
    val lastMinute: Int,
    val teams: List<TeamTimelineSeries>,
    /** 블루 − 레드. index = 분. */
    val teamGoldDiffByMinute: List<Int>,
    val participants: List<ParticipantTimelineSeries>,
    val kills: List<KillEntry>,
    val objectives: List<ObjectiveEntry>,
    val teamFights: List<TeamFightEntry>,
)

data class MapPointDto(
    val x: Int,
    val y: Int,
)

data class TeamTimelineSeries(
    val teamId: Int,
    val win: Boolean,
    /** 팀 다섯 명의 합. index = 분. */
    val goldByMinute: List<Int>,
    val xpByMinute: List<Int>,
    val csByMinute: List<Int>,
    /** 팀이 얼마나 뭉쳐 다녔나. 작을수록 붙어 있다. 자기 기지 안의 프레임은 빠진다. */
    val spreadByMinute: List<Int?>,
    val avgSpread: Int?,
)

data class ParticipantTimelineSeries(
    /** 타임라인 프레임의 키(1~10). 프레임과 사람을 잇는 유일한 고리다. */
    val participantId: Int,
    val riotId: String,
    val champion: String,
    val championId: Int,
    val teamId: Int,
    val position: String,
    val win: Boolean,
    val goldByMinute: List<Int>,
    val xpByMinute: List<Int>,
    val csByMinute: List<Int>,
    val levelByMinute: List<Int>,
    /** 손에 든 골드. 뚝 떨어지는 분이 아이템을 산 시점이다. */
    val currentGoldByMinute: List<Int>,
    /** 같은 자리 상대와의 격차. 라인 상대가 없었으면 빈 목록. */
    val goldDiffByMinute: List<Int>,
    /** index = 분. null 은 그 프레임에 좌표가 없었다는 뜻이다. */
    val positionsByMinute: List<MapPointDto?>,
    /** 라인전 동안 자기 라인에 있던 비율. 정글·포지션 미상은 null. */
    val laneShareRate: Double?,
    /** 라인전 동안 자기 라인도 자기 기지도 아닌 곳에 있던 비율. 정글은 null. */
    val roamRate: Double?,
    val enemyHalfRate: Double,
    val counterJungleRate: Double,
    /** 위 비율들의 분모. 좌표가 있는 프레임 수(0분 제외)다. */
    val framesSampled: Int,
    val goldDiff15: Int?,
    val csDiff15: Int?,
    val xpDiff15: Int?,
    val earlyKills: Int,
    val earlyDeaths: Int,
    val earlyAssists: Int,
    val soloKills: Int,
    val firstDeathMs: Long?,
)

data class KillEntry(
    val timestampMs: Long,
    val minute: Int,
    /** 0이면 사람이 아니다 — 포탑이나 미니언이 막타를 쳤다. */
    val killerParticipantId: Int,
    val victimParticipantId: Int,
    val assistParticipantIds: List<Int>,
    /** 킬을 올린 팀. 희생자의 팀을 뒤집어 구한다(막타가 포탑일 수 있어 킬러로는 못 센다). */
    val killingTeamId: Int?,
    val at: MapPointDto?,
    /** [com.gijun.main.domain.match.service.MapRegion] 이름. 좌표가 없으면 null. */
    val region: String?,
)

/**
 * 오브젝트와 건물을 **"먹은 팀" 관점으로 통일한** 항목. 이벤트 띠가 이것만 보고 그려진다.
 *
 * 원본의 `BUILDING_KILL.teamId` 는 **파괴당한** 건물의 팀이라 그대로 내보내면 색이 반대로
 * 칠해진다. 여기서 한 번 뒤집어 [killingTeamId] 로 담는다.
 */
data class ObjectiveEntry(
    val timestampMs: Long,
    val minute: Int,
    /** DRAGON, BARON_NASHOR, RIFTHERALD, HORDE, TOWER_BUILDING, INHIBITOR_BUILDING … */
    val kind: String,
    /** 드래곤 원소(FIRE_DRAGON 등). 해당 없으면 빈 값. */
    val subType: String,
    /** 포탑의 라인(TOP_LANE 등). 해당 없으면 빈 값. */
    val lane: String,
    /** OUTER_TURRET, INNER_TURRET, BASE_TURRET, NEXUS_TURRET. 억제기·몬스터는 빈 값. */
    val towerType: String,
    val killingTeamId: Int?,
    val killerParticipantId: Int,
    val assistParticipantIds: List<Int>,
    val at: MapPointDto?,
)

/**
 * 한 번의 교전. 규칙과 상수는 [com.gijun.main.domain.match.service.TeamFightDetector] 에 있다.
 *
 * 킬 세 개 미만인 묶음도 담는다([isTeamFight] 가 false). 솔로킬·2인 교전은 "어디서 물렸나"에
 * 쓸모가 있어서, 거르는 건 화면 몫이다.
 */
data class TeamFightEntry(
    val startMs: Long,
    val endMs: Long,
    val startMinute: Int,
    val team100Kills: Int,
    val team200Kills: Int,
    /** 킬을 더 많이 올린 팀. 동수면 null — 교환으로 끝난 교전이다. */
    val winnerTeamId: Int?,
    val openedByTeamId: Int?,
    val participantIds: List<Int>,
    val at: MapPointDto?,
    val region: String?,
    val isTeamFight: Boolean,
    /** 교전 직후 넘어간 오브젝트의 종류. 전리품이다. */
    val objectiveKinds: List<String>,
)
