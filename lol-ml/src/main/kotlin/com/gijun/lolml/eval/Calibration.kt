package com.gijun.lolml.eval

class CalibrationBin(
    val from: Double,
    val to: Double,
    val count: Int,
    val meanPredicted: Double,
    val actualWinRate: Double,
)

/** 예측 확률을 구간으로 나눠, 구간마다 실제로 얼마나 이겼는지 본다. 표본이 없는 구간은 뺀다. */
fun calibrationTable(
    predicted: DoubleArray,
    actual: DoubleArray,
    bins: Int = 10,
): List<CalibrationBin> {
    require(predicted.size == actual.size)
    val width = 1.0 / bins
    return predicted.indices
        // 1.0 은 마지막 구간에 넣는다.
        .groupBy { (predicted[it] / width).toInt().coerceIn(0, bins - 1) }
        .toSortedMap()
        .map { (bin, indices) ->
            CalibrationBin(
                from = bin * width,
                to = (bin + 1) * width,
                count = indices.size,
                meanPredicted = indices.map { predicted[it] }.average(),
                actualWinRate = indices.map { actual[it] }.average(),
            )
        }
}
