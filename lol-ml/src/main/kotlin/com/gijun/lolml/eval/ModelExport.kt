package com.gijun.lolml.eval

import com.gijun.lolml.feature.Example
import com.gijun.lolml.feature.PlayerState
import com.gijun.lolml.feature.TierPrior
import java.time.Instant

/**
 * 서비스에 넘길 모델: "자리 라인 승률 + 티어" 두 개짜리 로지스틱 회귀.
 *
 * **학습은 여기서 하고 서비스는 이 숫자로 계산만 한다.**
 *
 * ```
 * P(블루 승) = sigmoid(seatLaneWinRateDiff × 계수 + tierDiff × 계수)
 * ```
 *
 * 표준화는 계수에 접어 넣는다(가중치 ÷ 표준편차). bias 도 평균 빼기도 없는 모델이라 그렇게 해도 같은 식이고,
 * 서비스가 표준편차를 따로 알 필요가 없어진다.
 *
 * 피처를 만드는 법까지 넘기지는 못한다. 서비스는 같은 공식을 따로 구현하므로, 그쪽 테스트가 여기와 같은
 * 값을 내는지 본다. 공식에 들어가는 상수([PlayerState.PRIOR], 티어 평균)는 어긋나지 않게 같이 싣는다.
 */
object ModelExport {
    val FEATURES = listOf("seatLaneWinRateDiff", "tierDiff")

    /** @param examples 예열 경기를 뺀 전체. 평가가 아니라 내보낼 모델이므로 전부로 학습한다. */
    fun json(
        examples: List<Example>,
        tiers: TierPrior,
        lastGameCreation: Long,
    ): String {
        val fitted = Fitted.train(examples, FEATURES, symmetric = true)
        val coefficients = FEATURES.indices.map { fitted.model.weights[it] / fitted.standardizer.std[it] }
        return """
            {
              "seatLaneWinRateCoefficient": ${coefficients[0]},
              "tierCoefficient": ${coefficients[1]},
              "shrinkPrior": ${PlayerState.PRIOR},
              "tierMean": ${tiers.mean},
              "trainedMatches": ${examples.size},
              "lastGameCreation": $lastGameCreation,
              "exportedAt": "${Instant.now()}"
            }
            """.trimIndent() + "\n"
    }
}
