package com.gijun.main.domain.model.match

/**
 * LCU `game-timelines` 응답에서 지금 쓰는 만큼만 뽑아낸 모습.
 *
 * 원본(jsonb)은 그대로 보관한다. 여기 없는 필드(좌표, 아이템)를 나중에 쓰고 싶어지면
 * 다시 파싱하면 되기 때문에, 이 클래스는 "지금 쓰는 것"만 담는다.
 */
data class MatchTimeline(
    /** timestamp 오름차순. 비어 있으면 타임라인이 없는 것과 같다. */
    val frames: List<TimelineFrame>,
    /** timestamp 오름차순. 프레임마다 흩어져 오는 것을 한 줄로 편다. */
    val events: List<TimelineEvent> = emptyList(),
) {
    val isEmpty: Boolean get() = frames.isEmpty()
}

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
) {
    val cs: Int get() = minionsKilled + jungleMinionsKilled
}

/**
 * 타임라인 이벤트. LCU 는 이 세 종류만 준다 (Riot API 의 와드·아이템·레벨업 이벤트는 없다).
 *
 * participantId 가 0 이면 사람이 아니라는 뜻이다 — 포탑·미니언이 막타를 친 경우.
 */
sealed interface TimelineEvent {
    val timestampMs: Long

    data class ChampionKill(
        override val timestampMs: Long,
        val killerId: Int,
        val victimId: Int,
        val assistIds: List<Int>,
    ) : TimelineEvent

    data class EliteMonsterKill(
        override val timestampMs: Long,
        val killerId: Int,
        /** DRAGON, BARON_NASHOR, RIFTHERALD, HORDE(공허 유충), ATAKHAN … */
        val monsterType: String,
    ) : TimelineEvent

    data class BuildingKill(
        override val timestampMs: Long,
        val killerId: Int,
        /** **파괴당한** 건물의 팀. 부순 팀이 아니다. */
        val buildingTeamId: Int,
        /** TOWER_BUILDING, INHIBITOR_BUILDING */
        val buildingType: String,
    ) : TimelineEvent
}
