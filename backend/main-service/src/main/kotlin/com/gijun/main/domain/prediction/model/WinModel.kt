package com.gijun.main.domain.prediction.model

import kotlin.math.exp

/**
 * 픽 전 승률 예측 모델. **학습은 `lol-ml` 이 하고 여기는 그 숫자로 계산만 한다.**
 *
 * ```
 * P(블루 승) = sigmoid(자리 라인 승률 차 × 계수 + 티어 차 × 계수)
 * ```
 *
 * 두 값 모두 (블루 팀 평균 − 레드 팀 평균) 이다. bias 가 없어서 두 팀을 맞바꾸면 정확히 1 − p 가 된다.
 * 값은 `resources/ml/win-model.json` 에서 온다 — 다시 학습하면 `lol-ml` 의 `export` 가 그 파일을 새로 쓴다.
 *
 * 편성 화면의 기대 승률(자리 Elo)과는 다른 계산이다. 그쪽은 이 모델을 쓰지 않는다.
 */
data class WinModel(
    val seatLaneWinRateCoefficient: Double,
    val tierCoefficient: Double,
    /** 자리 라인 승률을 기본값 쪽으로 당기는 세기. 학습 때 쓴 값 그대로여야 한다. */
    val shrinkPrior: Double,
    /** 학습 때 티어를 모르는 사람에게 준 점수(아는 사람들의 평균). 여기서도 모르면 이 값을 쓴다. */
    val tierMean: Double,
    val trainedMatches: Int,
    /** 학습에 들어간 마지막 경기의 시작 시각(epoch millis). */
    val lastGameCreation: Long,
) {
    fun blueWinProbability(
        seatLaneWinRateDiff: Double,
        tierDiff: Double,
    ): Double = sigmoid(seatLaneWinRateCoefficient * seatLaneWinRateDiff + tierCoefficient * tierDiff)

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))
}
