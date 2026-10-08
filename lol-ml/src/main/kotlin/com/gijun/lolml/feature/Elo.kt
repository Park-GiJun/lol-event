package com.gijun.lolml.feature

import kotlin.math.pow

/** 서비스의 Elo 를 쓰지 않고 여기서 다시 계산한다. */
object Elo {
    const val INITIAL = 1500.0
    const val K = 32.0

    /** A 가 이길 기대 확률. 점수가 같으면 0.5, A 가 400 높으면 약 0.91. */
    fun expected(
        ratingA: Double,
        ratingB: Double,
    ): Double = 1.0 / (1.0 + 10.0.pow((ratingB - ratingA) / 400.0))

    /**
     * 결과를 반영한 새 점수. [score] 는 이기면 1.0, 지면 0.0.
     * 이길 줄 알았는데(expected 0.9) 이기면 조금 오르고, 질 줄 알았는데(0.1) 이기면 많이 오른다.
     */
    fun updated(
        rating: Double,
        expected: Double,
        score: Double,
    ): Double = rating + K * (score - expected)
}
