package com.gijun.lolml.model

import com.gijun.lolml.feature.Example

class Split(
    val train: List<Example>,
    val test: List<Example>,
)

/** 앞쪽 [trainRatio] 가 학습, 나머지 최근 경기가 테스트. 섞지 않는다. */
fun timeSplit(
    examples: List<Example>,
    trainRatio: Double = 0.8,
): Split = TODO()
