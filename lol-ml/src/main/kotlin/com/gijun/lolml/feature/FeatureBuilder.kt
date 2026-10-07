package com.gijun.lolml.feature

import com.gijun.lolml.data.Match

/** 학습 표본 하나. [features] 의 순서는 [FeatureBuilder.NAMES] 와 같다. */
class Example(
    val matchId: String,
    val features: DoubleArray,
    /** 블루 승 = 1.0 */
    val label: Double,
)

/**
 * 경기를 시간순으로 재생하며 피처를 만든다.
 *
 * 한 경기에서 순서는 반드시: **지금 상태로 피처 생성 → 그 다음 이 경기 결과로 상태 갱신.**
 * 뒤집히면 그 경기의 결과가 그 경기의 피처에 샌다.
 */
class FeatureBuilder {
    private val states = HashMap<String, PlayerState>()

    /** [matches] 는 시간순이어야 한다. */
    fun build(matches: List<Match>): List<Example> = TODO()

    companion object {
        /** 전부 (블루 팀 평균 − 레드 팀 평균). */
        val NAMES = listOf("eloDiff", "recentKdaDiff", "logGamesDiff")
    }
}
