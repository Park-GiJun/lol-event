package com.gijun.lolml.model

import kotlin.math.exp

/**
 * p = sigmoid(w·x + b), 손실 = log loss + (l2 / 2)·|w|².
 * 배치 경사하강법으로 직접 푼다.
 */
class LogisticRegression(
    private val learningRate: Double,
    private val l2: Double,
    private val epochs: Int,
    /**
     * false 면 bias 를 0 에 묶는다. 피처가 전부 (블루 − 레드) 이고 진영에 유불리가 없다면,
     * 두 팀을 맞바꿨을 때 확률이 정확히 1 − p 가 되어야 한다. bias 가 있으면 그게 깨진다.
     */
    private val fitBias: Boolean = true,
) {
    var weights: DoubleArray = DoubleArray(0)
        private set
    var bias: Double = 0.0
        private set

    fun fit(
        x: List<DoubleArray>,
        y: DoubleArray,
    ) {
        require(x.isNotEmpty() && x.size == y.size) { "표본 수가 맞지 않는다: x=${x.size}, y=${y.size}" }
        val n = x.size
        val dim = x.first().size
        weights = DoubleArray(dim)
        bias = 0.0

        repeat(epochs) {
            // 가중치마다 "이걸 올리면 손실이 얼마나 늘어나는가"(기울기)를 모은다.
            val gradW = DoubleArray(dim)
            var gradB = 0.0
            for (i in 0 until n) {
                // 예측 − 실제. 양수면 너무 높게 불렀다는 뜻이다.
                val error = predict(x[i]) - y[i]
                // 그 경기에서 값이 컸던 피처일수록 이 오차에 책임이 크다.
                for (j in 0 until dim) gradW[j] += error * x[i][j]
                gradB += error
            }
            // 기울기의 반대쪽으로 한 걸음. l2 항은 가중치를 0 쪽으로 조금씩 당긴다(bias 는 당기지 않는다).
            for (j in 0 until dim) {
                weights[j] -= learningRate * (gradW[j] / n + l2 * weights[j])
            }
            if (fitBias) bias -= learningRate * (gradB / n)
        }
    }

    fun predict(x: DoubleArray): Double {
        var z = bias
        for (j in weights.indices) z += weights[j] * x[j]
        return sigmoid(z)
    }
}

/** 어떤 수든 0~1 사이로 누른다. 0 이면 0.5. 큰 음수에서 exp 가 넘치지 않게 둘로 나눠 계산한다. */
fun sigmoid(z: Double): Double =
    if (z >= 0) {
        1.0 / (1.0 + exp(-z))
    } else {
        val e = exp(z)
        e / (1.0 + e)
    }
