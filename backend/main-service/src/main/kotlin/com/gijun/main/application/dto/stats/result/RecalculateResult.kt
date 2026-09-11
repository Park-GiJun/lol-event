package com.gijun.main.application.dto.stats.result

/**
 * 전체 재집계 결과 요약. 재집계가 실제로 무엇을 셌는지 눈으로 확인하기 위한 값이다.
 *
 * [ratedMatches] 가 기대보다 적으면 재생 대상 필터(10명 · 600초 초과)에 걸린 경기가 있다는 뜻이고,
 * [laneDuels] 가 `ratedMatches × 5` 에 못 미치면 포지션 배정이 깨진 팀이 있다는 뜻이다.
 */
data class RecalculateResult(
    val totalMatches: Int,
    val ratedMatches: Int,
    val players: Int,
    val laneDuels: Int,
    /** LaneMethod 이름 -> 그 방법으로 판정한 경기 수. */
    val methodCounts: Map<String, Int>,
)
