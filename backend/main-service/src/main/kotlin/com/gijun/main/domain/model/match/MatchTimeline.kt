package com.gijun.main.domain.model.match

/**
 * LCU `game-timelines` 응답에서 지금 쓰는 만큼만 뽑아낸 모습.
 *
 * 원본(jsonb)은 그대로 보관한다. 여기 없는 필드를 나중에 쓰고 싶어지면 다시 파싱하면 되기 때문에,
 * 이 클래스는 "지금 쓰는 것"만 담는다. 무엇을 **일부러 안 읽는지**는
 * [com.gijun.main.domain.service.TimelineParser] 의 KDoc 에 이유와 함께 적어 뒀다.
 */
data class MatchTimeline(
    /** timestamp 오름차순. 비어 있으면 타임라인이 없는 것과 같다. */
    val frames: List<TimelineFrame>,
    /** timestamp 오름차순. 프레임마다 흩어져 오는 것을 한 줄로 편다. */
    val events: List<TimelineEvent> = emptyList(),
) {
    val isEmpty: Boolean get() = frames.isEmpty()
}

/**
 * 소환사의 협곡 미니맵 좌표. 블루 본진이 (작은 x, 작은 y), 레드 본진이 반대편이다.
 *
 * 저장된 17판 실측 범위는 x 130~14,589 / y 135~14,673 이었다. 통설상 맵 한계는 14,870 이지만
 * 실제로 밟히는 영역은 그보다 좁다 — 영역 판정 상수는
 * [com.gijun.main.domain.service.MapGeometry] 에서 실측값을 근거로 정한다.
 */
data class MapPoint(val x: Int, val y: Int)

data class TimelineFrame(
    /** 경기 시작 기준 경과 시간(ms). */
    val timestampMs: Long,
    /** participantId(1~10) -> 그 시점 누적치. */
    val participants: Map<Int, ParticipantFrame>,
)

data class ParticipantFrame(
    val participantId: Int,
    val totalGold: Int,
    val xp: Int,
    val level: Int = 0,
    val minionsKilled: Int = 0,
    val jungleMinionsKilled: Int = 0,
    /** 그 시점 **손에 든** 골드. 아이템을 사면 떨어진다. 누적치인 [totalGold] 와 다르다. */
    val currentGold: Int = 0,
    /**
     * 그 시점 좌표. null 이면 원본에 없었다는 뜻이다 — 좌표 기반 비율 지표는 이 프레임을
     * 분모에서도 빼야 한다. (0,0) 을 기본값으로 쓰지 않는 이유가 이것이다: 그러면 "없음"과
     * "맵 왼쪽 아래 끝"을 구분할 수 없고 분모가 조용히 오염된다.
     */
    val position: MapPoint? = null,
) {
    val cs: Int get() = minionsKilled + jungleMinionsKilled
}

/**
 * 타임라인 이벤트. LCU 는 이 세 종류만 준다 (Riot API 의 와드·아이템·레벨업 이벤트는 없다).
 *
 * 저장된 17판을 전수 쿼리해 확인한 결과다 — `CHAMPION_KILL` 1,100건, `BUILDING_KILL` 244건,
 * `ELITE_MONSTER_KILL` 153건, **그 외 타입 0건**. 그래서 아이템 빌드 순서·스킬 선마·와드
 * 히트맵·포탑 플레이트는 이 데이터로는 만들 수 없다. 수집 소스를 Riot match-v5 로 바꾸지 않는
 * 한 영구히 불가능하고, 과거 경기 백필도 안 된다(LCU 히스토리에 타임라인이 없다).
 *
 * 원본은 타입별 스키마가 아니라 **15개 필드 공용 union** 이다. 타입에 안 맞는 필드는 0이나
 * 빈 값으로 채워져 온다 — 필드가 있다고 값이 있는 게 아니다.
 *
 * participantId 가 0 이면 사람이 아니라는 뜻이다 — 포탑·미니언이 막타를 친 경우.
 */
sealed interface TimelineEvent {
    val timestampMs: Long

    /**
     * 이벤트가 일어난 좌표. 세 타입 **모두** 원본에 갖고 있어서 공통 멤버로 올렸다
     * (킬·데스 지도와 오브젝트 위치를 타입 분기 없이 모을 수 있다).
     *
     * null 은 부재를 뜻한다. 17판 실측에서는 좌표가 빠진 이벤트가 없었지만, 원본이 바뀌어도
     * 조용히 (0,0) 으로 세지 않도록 방어해 둔다.
     */
    val position: MapPoint?

    data class ChampionKill(
        override val timestampMs: Long,
        val killerId: Int,
        val victimId: Int,
        val assistIds: List<Int>,
        override val position: MapPoint? = null,
    ) : TimelineEvent

    data class EliteMonsterKill(
        override val timestampMs: Long,
        val killerId: Int,
        /** DRAGON, BARON_NASHOR, RIFTHERALD, HORDE(공허 유충), ATAKHAN … */
        val monsterType: String,
        /**
         * 드래곤 원소. 17판에서 관측된 값은 HEXTECH / WATER / AIR / EARTH / CHEMTECH /
         * FIRE_DRAGON 6종이다 (ELDER_DRAGON 은 아직 없었다). 드래곤이 아닌 오브젝트는 빈 값이다.
         */
        val monsterSubType: String = "",
        val assistIds: List<Int> = emptyList(),
        override val position: MapPoint? = null,
    ) : TimelineEvent

    data class BuildingKill(
        override val timestampMs: Long,
        val killerId: Int,
        /** **파괴당한** 건물의 팀. 부순 팀이 아니다. */
        val buildingTeamId: Int,
        /** TOWER_BUILDING, INHIBITOR_BUILDING */
        val buildingType: String,
        /** OUTER_TURRET, INNER_TURRET, BASE_TURRET, NEXUS_TURRET. 억제기는 빈 값이다. */
        val towerType: String = "",
        /** TOP_LANE, MID_LANE, BOT_LANE. 쌍둥이 포탑은 MID_LANE 으로 기록돼 온다. */
        val laneType: String = "",
        val assistIds: List<Int> = emptyList(),
        override val position: MapPoint? = null,
    ) : TimelineEvent
}
