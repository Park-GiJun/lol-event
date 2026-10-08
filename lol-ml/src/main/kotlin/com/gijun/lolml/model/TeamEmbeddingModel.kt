package com.gijun.lolml.model

import com.gijun.lolml.autograd.Mlp
import com.gijun.lolml.autograd.Value
import com.gijun.lolml.autograd.step
import com.gijun.lolml.autograd.zeroGrad
import com.gijun.lolml.feature.Example
import kotlin.random.Random

/**
 * 사람마다 숫자 [dim] 개(임베딩)를 두고, **누가 누구와 같은 팀이었고 이겼는지만** 보고 그 숫자를 배운다.
 *
 * 1단계의 피처(Elo, 승률 …)는 사람이 "이걸 세면 도움이 되겠지" 하고 정한 것이다. 여기서는 정하지 않는다.
 * 숫자의 뜻은 비워 두고, 경기 결과를 잘 맞추는 쪽으로 기울기를 따라 움직이게 둔다.
 *
 * ```
 * 팀 벡터  = 다섯 명 임베딩의 평균
 * 팀 힘    = MLP(팀 벡터)                  ← 가운데 층이 "이런 사람들이 같이 있으면" 을 배운다(팀 케미)
 * P(블루 승) = sigmoid(블루 힘 − 레드 힘)   ← 두 팀을 맞바꾸면 정확히 1 − p
 * ```
 *
 * 배울 숫자가 (사람 수 × [dim]) + MLP 가중치라 경기 수보다 많다. 과적합하기 쉬워서 [l2] 로
 * 숫자들을 0 쪽으로 당긴다. 처음 보는 사람은 임베딩이 0 = "평균적인 사람" 이다.
 */
class TeamEmbeddingModel(
    private val dim: Int = 4,
    private val hidden: Int = 4,
    private val epochs: Int = 150,
    private val learningRate: Double = 0.3,
    private val l2: Double = 0.01,
    private val seed: Int = 0,
) {
    private val embeddings = HashMap<String, List<Value>>()
    private var mlp = Mlp(dim, listOf(hidden, 1), Random(seed))
    private val unknown = List(dim) { Value(0.0) }

    val parameterCount: Int get() = parameters().size

    fun fit(train: List<Example>) {
        val random = Random(seed)
        mlp = Mlp(dim, listOf(hidden, 1), random)
        embeddings.clear()
        // 0 근처의 작은 값에서 시작한다. 전부 같은 값이면 모두가 똑같이 움직여 구별이 생기지 않는다.
        for (example in train) {
            for (player in example.blue + example.red) {
                embeddings.getOrPut(player) { List(dim) { Value(random.nextDouble(-INIT, INIT)) } }
            }
        }
        val parameters = parameters()

        repeat(epochs) {
            val losses =
                train.map { example ->
                    val z = score(example)
                    // log loss. 블루가 이겼으면 −ln(p), 졌으면 −ln(1 − p). 1 − sigmoid(z) = sigmoid(−z) 다.
                    if (example.label == 1.0) -z.sigmoid().ln() else -(-z).sigmoid().ln()
                }
            val loss = Value.sum(losses) / train.size.toDouble()

            parameters.zeroGrad()
            // 1단계에서는 이 자리에 손으로 유도한 기울기 공식이 있었다.
            loss.backward()
            parameters.step(learningRate, l2)
        }
    }

    fun predict(example: Example): Double = score(example).sigmoid().data

    /** 그 사람의 임베딩. 학습 때 본 적 없으면 null. */
    fun embedding(player: String): DoubleArray? = embeddings[player]?.let { values -> DoubleArray(dim) { values[it].data } }

    /** 블루 힘 − 레드 힘. */
    private fun score(example: Example): Value = strength(example.blue) - strength(example.red)

    private fun strength(team: List<String>): Value {
        val vectors = team.map { embeddings[it] ?: unknown }
        val teamVector = List(dim) { d -> Value.sum(vectors.map { it[d] }) / team.size.toDouble() }
        return mlp(teamVector).single()
    }

    private fun parameters(): List<Value> = embeddings.values.flatten() + mlp.parameters()

    private companion object {
        const val INIT = 0.1
    }
}
