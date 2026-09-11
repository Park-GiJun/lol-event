package com.gijun.main.domain.model.match

/**
 * LCU `game-timelines` 응답에서 라인 점수 계산에 필요한 만큼만 뽑아낸 모습.
 *
 * 원본(jsonb)은 그대로 보관한다. 여기 없는 필드(이벤트, 좌표, 아이템)를 나중에 쓰고 싶어지면
 * 다시 파싱하면 되기 때문에, 이 클래스는 "지금 쓰는 것"만 담는다.
 */
data class MatchTimeline(
    /** timestamp 오름차순. 비어 있으면 타임라인이 없는 것과 같다. */
    val frames: List<TimelineFrame>,
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
)
