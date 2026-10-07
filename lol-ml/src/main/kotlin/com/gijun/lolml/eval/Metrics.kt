package com.gijun.lolml.eval

/** −평균( y·ln p + (1−y)·ln(1−p) ). 상수 0.5 예측이면 0.693. */
fun logLoss(
    predicted: DoubleArray,
    actual: DoubleArray,
): Double = TODO()

/** 평균( (p − y)² ). 상수 0.5 예측이면 0.25. */
fun brierScore(
    predicted: DoubleArray,
    actual: DoubleArray,
): Double = TODO()
