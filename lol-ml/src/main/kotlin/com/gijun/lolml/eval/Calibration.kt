package com.gijun.lolml.eval

class CalibrationBin(
    val from: Double,
    val to: Double,
    val count: Int,
    val meanPredicted: Double,
    val actualWinRate: Double,
)

/** 예측 확률을 구간으로 나눠, 구간마다 실제로 얼마나 이겼는지 본다. */
fun calibrationTable(
    predicted: DoubleArray,
    actual: DoubleArray,
    bins: Int = 10,
): List<CalibrationBin> = TODO()
