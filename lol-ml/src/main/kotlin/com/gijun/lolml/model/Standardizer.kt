package com.gijun.lolml.model

/** 피처별 (x − 평균) / 표준편차. 통계는 학습셋에서만 구한다. */
class Standardizer private constructor(
    val mean: DoubleArray,
    val std: DoubleArray,
) {
    fun transform(features: DoubleArray): DoubleArray = TODO()

    companion object {
        fun fit(trainFeatures: List<DoubleArray>): Standardizer = TODO()
    }
}
