package com.gijun.lolml.autograd

import com.gijun.lolml.model.LogisticRegression
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValueTest {
    /** 기울기를 공식 없이 재는 법: x 를 아주 조금 움직여 보고 결과가 얼마나 움직였는지 본다. */
    private fun numericGrad(
        x: Double,
        f: (Double) -> Double,
    ): Double = (f(x + 1e-6) - f(x - 1e-6)) / 2e-6

    private fun assertGrad(
        x: Double,
        viaValue: (Value) -> Value,
        viaDouble: (Double) -> Double,
    ) {
        val input = Value(x)
        val out = viaValue(input)
        out.backward()
        assertEquals(viaDouble(x), out.data, 1e-9)
        assertEquals(numericGrad(x, viaDouble), input.grad, 1e-5)
    }

    @Test
    fun `연산마다 기울기가 조금 움직여 잰 값과 같다`() {
        assertGrad(1.7, { it + 3.0 }, { it + 3.0 })
        assertGrad(1.7, { it * 3.0 }, { it * 3.0 })
        assertGrad(1.7, { 2.0 - it }, { 2.0 - it })
        assertGrad(1.7, { it / 4.0 }, { it / 4.0 })
        assertGrad(1.7, { Value(4.0) / it }, { 4.0 / it })
        assertGrad(1.7, { it.pow(3.0) }, { it * it * it })
        assertGrad(1.7, { it.exp() }, { kotlin.math.exp(it) })
        assertGrad(1.7, { it.ln() }, { kotlin.math.ln(it) })
        assertGrad(0.4, { it.tanh() }, { kotlin.math.tanh(it) })
        assertGrad(-0.4, { it.sigmoid() }, { 1.0 / (1.0 + kotlin.math.exp(-it)) })
    }

    @Test
    fun `값이 두 군데에 쓰이면 기울기가 더해진다`() {
        val a = Value(3.0)
        val out = a * a + a
        out.backward()
        // a² + a 의 기울기는 2a + 1
        assertEquals(7.0, a.grad, 1e-12)
    }

    @Test
    fun `여러 단계를 거친 식도 거꾸로 따라가 맞춘다`() {
        assertGrad(
            0.8,
            { ((it * 2.0 + 1.0).tanh() * it).sigmoid() },
            { 1.0 / (1.0 + kotlin.math.exp(-(kotlin.math.tanh(it * 2 + 1) * it))) },
        )
    }

    @Test
    fun `sum 은 노드 하나로 더하고 값이 만 개여도 backward 가 터지지 않는다`() {
        val values = List(10_000) { Value(1.0) }
        val out = Value.sum(values) * 2.0
        out.backward()
        assertEquals(20_000.0, out.data, 1e-9)
        assertTrue(values.all { it.grad == 2.0 })

        var chain = Value(0.0)
        repeat(50_000) { chain = chain + 1.0 }
        chain.backward()
        assertEquals(50_000.0, chain.data, 1e-9)
    }

    /**
     * 1단계와 잇는 다리. 손으로 유도한 공식 `(예측 − 실제) × 피처` 로 학습한 로지스틱 회귀와,
     * 손실만 적어 주고 기울기는 autograd 에게 맡긴 것이 같은 답을 낸다.
     */
    @Test
    fun `autograd 로 학습한 로지스틱 회귀가 손으로 짠 것과 같은 가중치를 낸다`() {
        val random = Random(5)
        val x = List(40) { doubleArrayOf(random.nextDouble(-1.0, 1.0), random.nextDouble(-1.0, 1.0)) }
        val y = DoubleArray(x.size) { if (random.nextDouble() < 0.5 + 0.3 * x[it][0]) 1.0 else 0.0 }
        val learningRate = 0.5
        val l2 = 0.1
        val epochs = 50

        val byHand = LogisticRegression(learningRate, l2, epochs).also { it.fit(x, y) }

        val weights = List(2) { Value(0.0) }
        val bias = Value(0.0)
        repeat(epochs) {
            val losses =
                x.indices.map { i ->
                    val p = (weights[0] * x[i][0] + weights[1] * x[i][1] + bias).sigmoid()
                    // log loss 를 그대로 적는다. 미분은 하지 않는다.
                    -(p.ln() * y[i] + (1.0 - p).ln() * (1.0 - y[i]))
                }
            val penalty = (weights[0] * weights[0] + weights[1] * weights[1]) * (l2 / 2)
            val loss = Value.sum(losses) / x.size.toDouble() + penalty

            (weights + bias).zeroGrad()
            loss.backward()
            (weights + bias).step(learningRate)
        }

        assertEquals(byHand.weights[0], weights[0].data, 1e-9)
        assertEquals(byHand.weights[1], weights[1].data, 1e-9)
        assertEquals(byHand.bias, bias.data, 1e-9)
    }

    /**
     * XOR — 둘 중 **하나만** 1 일 때 1. 직선 하나로는 가를 수 없어서 로지스틱 회귀는 못 푼다.
     * 가운데 층이 있으면 푼다.
     */
    @Test
    fun `가운데 층이 있는 신경망은 XOR 을 배운다`() {
        val inputs = listOf(listOf(0.0, 0.0), listOf(0.0, 1.0), listOf(1.0, 0.0), listOf(1.0, 1.0))
        val targets = listOf(0.0, 1.0, 1.0, 0.0)
        val mlp = Mlp(inputs = 2, sizes = listOf(8, 1), random = Random(1))

        fun predict(input: List<Double>): Value = mlp(input.map { Value(it) }).single().sigmoid()

        fun loss(): Value =
            Value.sum(
                inputs.indices.map { i ->
                    val p = predict(inputs[i])
                    -(p.ln() * targets[i] + (1.0 - p).ln() * (1.0 - targets[i]))
                },
            ) / inputs.size.toDouble()

        val before = loss().data
        repeat(2000) {
            val current = loss()
            mlp.parameters().zeroGrad()
            current.backward()
            mlp.parameters().step(0.5)
        }

        assertTrue(loss().data < 0.05, "손실이 충분히 내려가야 한다: $before → ${loss().data}")
        inputs.indices.forEach { i -> assertEquals(targets[i], predict(inputs[i]).data, 0.1) }
    }
}
