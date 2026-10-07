package com.gijun.lolml.model

/**
 * p = sigmoid(w·x + b), 손실 = log loss + (l2 / 2)·|w|².
 * 배치 경사하강법으로 직접 푼다.
 */
class LogisticRegression(
    private val learningRate: Double,
    private val l2: Double,
    private val epochs: Int,
) {
    var weights: DoubleArray = DoubleArray(0)
        private set
    var bias: Double = 0.0
        private set

    fun fit(
        x: List<DoubleArray>,
        y: DoubleArray,
    ): Unit = TODO()

    fun predict(x: DoubleArray): Double = TODO()
}

fun sigmoid(z: Double): Double = TODO()
