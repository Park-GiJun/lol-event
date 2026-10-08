package com.gijun.lolml.model

import com.gijun.lolml.feature.Example

/**
 * 경기 i 를 맞출 때 그 앞의 경기(0 until i)로만 학습한다. 이것을 [minTrain] 번째 경기부터 끝까지 되풀이한다.
 *
 * 앞 80% / 뒤 20% 로 한 번 자르면 평가에 쓰는 경기가 20% 뿐이다. 이렇게 하면 미래를 보지 않으면서도
 * [minTrain] 뒤의 모든 경기가 한 번씩 시험 문제가 된다.
 *
 * @param fit 학습 표본을 받아 "경기 하나 → 블루 승 확률" 을 돌려준다.
 * @return `examples.drop(minTrain)` 과 같은 순서의 예측.
 */
fun walkForward(
    examples: List<Example>,
    minTrain: Int,
    fit: (train: List<Example>) -> (Example) -> Double,
): DoubleArray {
    require(minTrain in 1 until examples.size) { "minTrain 은 1 이상 ${examples.size} 미만이어야 한다: $minTrain" }
    return DoubleArray(examples.size - minTrain) { k ->
        val i = minTrain + k
        fit(examples.subList(0, i))(examples[i])
    }
}
