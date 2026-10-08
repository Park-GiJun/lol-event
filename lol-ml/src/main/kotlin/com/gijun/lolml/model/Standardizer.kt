package com.gijun.lolml.model

import kotlin.math.sqrt

/** 피처별 (x − 평균) / 표준편차. 통계는 학습셋에서만 구한다. */
class Standardizer private constructor(
    val mean: DoubleArray,
    val std: DoubleArray,
) {
    fun transform(features: DoubleArray): DoubleArray = DoubleArray(features.size) { j -> (features[j] - mean[j]) / std[j] }

    companion object {
        /**
         * @param center false 면 평균을 빼지 않고 크기만 맞춘다(0 을 기준으로 한 흔들림 폭으로 나눈다).
         *   (블루 − 레드) 피처는 0 이 "두 팀이 같다" 는 뜻이라, 평균을 빼면 그 뜻이 옮겨 간다.
         */
        fun fit(
            trainFeatures: List<DoubleArray>,
            center: Boolean = true,
        ): Standardizer {
            require(trainFeatures.isNotEmpty()) { "학습 표본이 없다" }
            val n = trainFeatures.size
            val dim = trainFeatures.first().size
            val mean = DoubleArray(dim) { j -> if (center) trainFeatures.sumOf { it[j] } / n else 0.0 }
            val std =
                DoubleArray(dim) { j ->
                    val variance = trainFeatures.sumOf { (it[j] - mean[j]) * (it[j] - mean[j]) } / n
                    // 값이 전부 같은 피처는 0 으로 나누게 된다. 1 로 두면 변환 결과가 0 이 된다.
                    sqrt(variance).takeIf { it > 0.0 } ?: 1.0
                }
            return Standardizer(mean, std)
        }
    }
}
