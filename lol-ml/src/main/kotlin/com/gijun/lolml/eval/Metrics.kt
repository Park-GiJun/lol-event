package com.gijun.lolml.eval

import kotlin.math.ln

private const val EPSILON = 1e-15

/** −평균( y·ln p + (1−y)·ln(1−p) ). 상수 0.5 예측이면 0.693. */
fun logLoss(
    predicted: DoubleArray,
    actual: DoubleArray,
): Double {
    require(predicted.isNotEmpty() && predicted.size == actual.size)
    var sum = 0.0
    for (i in predicted.indices) {
        // p 가 정확히 0 이나 1 이면 ln(0) 이 −무한대가 된다. 아주 조금 안쪽으로 민다.
        val p = predicted[i].coerceIn(EPSILON, 1.0 - EPSILON)
        sum += actual[i] * ln(p) + (1.0 - actual[i]) * ln(1.0 - p)
    }
    return -sum / predicted.size
}

/** 평균( (p − y)² ). 상수 0.5 예측이면 0.25. */
fun brierScore(
    predicted: DoubleArray,
    actual: DoubleArray,
): Double {
    require(predicted.isNotEmpty() && predicted.size == actual.size)
    var sum = 0.0
    for (i in predicted.indices) sum += (predicted[i] - actual[i]) * (predicted[i] - actual[i])
    return sum / predicted.size
}
