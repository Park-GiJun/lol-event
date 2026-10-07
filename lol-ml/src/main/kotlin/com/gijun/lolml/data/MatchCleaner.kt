package com.gijun.lolml.data

/**
 * 정제 규칙. 원본 스냅샷은 건드리지 않고 여기서 걸러낸 결과만 학습에 쓴다.
 * 순서(시간순)는 유지한다.
 */
class MatchCleaner(
    /** 이보다 짧으면 다시하기·초반 AFK 로 본다. 값은 `game_duration` 분포를 보고 정한다. */
    private val minDurationSec: Int,
    /** 내전 큐만 남긴다. */
    private val queueIds: Set<Int>,
) {
    fun clean(matches: List<Match>): List<Match> = TODO("큐 필터 → 짧은 경기 제외 → 중복 제거 → 5:5 가 아닌 경기 제외")
}
