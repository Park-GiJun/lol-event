package com.gijun.main.application.dto.stats.result

/**
 * 워크포워드 검증 결과.
 *
 * **이 내전은 사전 데이터로 승패를 잘 맞힐 수 없는 구조다.** 매 세션 한 사람이 포지션까지 보고
 * 팀을 짜기 때문에, 편성자가 균형을 맞춘 만큼 남는 건 실력차가 아니라 편성자의 추정 오차다.
 * 숫자가 낮다고 버그로 의심하지 마라. 기대치는 아래와 같다.
 *
 * - 라인 Elo 의 팀 승패 적중률: 약 63% (세션 첫 경기 기준)
 * - 같은 팀 분할이 반복된 21쌍에서 같은 팀이 재승리한 비율 66.7% → 팀 구성을 완벽히 알아도
 *   이론적 상한이 약 79%, 현실적으로 65~70%
 * - 특징 28개로 로지스틱·GBM·랜덤포레스트를 돌려도 전부 무정보 기준선보다 로그로스가 나빴다
 */
data class RatingValidationResult(
    val totalMatches: Int,
    /** 재생 대상 필터(10명 · 600초 초과)를 통과한 경기. */
    val ratableMatches: Int,
    /** 예열에 쓴 경기 수. 이 경기들은 레이팅을 움직이지만 평가에는 들어가지 않는다. */
    val warmup: Int,
    /** 6시간 이상 간격으로 끊어 센 세션 수. */
    val sessions: Int,
    /** 직전 경기와 팀 구성이 같아 평가에서 뺀 경기 수. */
    val excludedRepeatedTeams: Int,
    /** LaneMethod 이름 -> 경기 수. 방법이 섞여 있으면 여기서 보인다. */
    val methodCounts: Map<String, Int>,

    /** 평가 대상 전체. */
    val overall: ValidationScope,
    /**
     * 세션의 첫 경기만. **이쪽이 더 정직한 숫자다** — 같은 세션 안에서는 팀 구성이 반복되기 쉬워
     * "직전 승자가 또 이긴다"는 누수가 섞인다.
     */
    val sessionFirst: ValidationScope,

    val splitHalf: SplitHalfReliability,
)

/** 한 평가 구간에서 세 가지 예측기를 나란히 비교한다. */
data class ValidationScope(
    val games: Int,
    /** 무정보 기준선(항상 0.5). 로그로스 ln2 = 0.6931. 이보다 나쁘면 그 예측기는 해롭다. */
    val baseline: PredictionMetrics,
    /** 라인 레이팅 팀 평균으로 승패를 예측. */
    val laneElo: PredictionMetrics,
    /** 전적 레이팅 팀 평균으로 승패를 예측. */
    val teamElo: PredictionMetrics,
)

data class PredictionMetrics(
    val logLoss: Double,
    val accuracy: Double,
)

/**
 * 반분 신뢰도 — 같은 사람의 라인 맞대결을 홀수/짝수로 반 갈라 각각 승률을 내고, 그 둘이
 * 사람들 사이에서 얼마나 같이 움직이는지 본다. 측정하고 있는 것이 실력이라면 두 반쪽이
 * 같은 방향을 가리켜야 한다.
 *
 * 현재 LEGACY_FINAL 기준 0.568. 타임라인 경기가 40개쯤 쌓였을 때 이 값이 오르면 확정 개선이다.
 * K 가 커질수록 이 값은 단조 감소한다 (K=8 에서 0.629 → K=64 에서 0.470).
 */
data class SplitHalfReliability(
    /** 양쪽 반에 [minDuelsPerHalf] 이상을 가진 사람 수. */
    val players: Int,
    val minDuelsPerHalf: Int,
    /** 두 반쪽 승률의 피어슨 상관. */
    val correlation: Double,
    /** 스피어만-브라운 보정값 `2r/(1+r)`. 전체 길이 기준으로 환산한 신뢰도. */
    val spearmanBrown: Double,
)
