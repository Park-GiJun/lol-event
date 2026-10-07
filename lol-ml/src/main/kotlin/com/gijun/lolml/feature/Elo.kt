package com.gijun.lolml.feature

/** 서비스의 Elo 를 쓰지 않고 여기서 다시 계산한다. */
object Elo {
    const val INITIAL = 1500.0
    const val K = 32.0

    /** A 가 이길 기대 확률. */
    fun expected(
        ratingA: Double,
        ratingB: Double,
    ): Double = TODO("1 / (1 + 10^((B - A) / 400))")

    /** 결과를 반영한 새 점수. [score] 는 이기면 1.0, 지면 0.0. */
    fun updated(
        rating: Double,
        expected: Double,
        score: Double,
    ): Double = TODO("rating + K * (score - expected)")
}
