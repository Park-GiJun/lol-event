package com.gijun.lolml.model

import com.gijun.lolml.eval.logLoss
import com.gijun.lolml.feature.Example
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogisticRegressionTest {
    @Test
    fun `sigmoid 는 0 에서 0_5 이고 큰 수에서도 넘치지 않는다`() {
        assertEquals(0.5, sigmoid(0.0), 1e-12)
        assertEquals(1.0, sigmoid(1000.0), 1e-12)
        assertEquals(0.0, sigmoid(-1000.0), 1e-12)
        assertEquals(1.0, sigmoid(2.0) + sigmoid(-2.0), 1e-12)
    }

    /** 답을 아는 데이터를 만들어 학습시키고, 그 답을 되찾는지 본다. */
    @Test
    fun `데이터를 만든 가중치를 되찾는다`() {
        val random = Random(42)
        val x = List(5000) { doubleArrayOf(random.nextDouble(-2.0, 2.0), random.nextDouble(-2.0, 2.0)) }
        val y = DoubleArray(x.size) { if (random.nextDouble() < sigmoid(2.0 * x[it][0] - 1.0 * x[it][1] + 0.5)) 1.0 else 0.0 }

        val model = LogisticRegression(learningRate = 0.5, l2 = 0.0, epochs = 2000)
        model.fit(x, y)

        assertEquals(2.0, model.weights[0], 0.2)
        assertEquals(-1.0, model.weights[1], 0.2)
        assertEquals(0.5, model.bias, 0.2)
    }

    @Test
    fun `학습하면 찍는 것보다 손실이 낮고 L2 는 가중치를 줄인다`() {
        val random = Random(7)
        val x = List(500) { doubleArrayOf(random.nextDouble(-1.0, 1.0)) }
        val y = DoubleArray(x.size) { if (random.nextDouble() < sigmoid(3.0 * x[it][0])) 1.0 else 0.0 }

        val plain = LogisticRegression(0.5, l2 = 0.0, epochs = 1000).also { it.fit(x, y) }
        val shrunk = LogisticRegression(0.5, l2 = 1.0, epochs = 1000).also { it.fit(x, y) }

        assertTrue(logLoss(DoubleArray(x.size) { plain.predict(x[it]) }, y) < 0.6931)
        assertTrue(shrunk.weights[0] > 0.0 && shrunk.weights[0] < plain.weights[0])
    }

    @Test
    fun `표준화는 학습셋을 평균 0 표준편차 1 로 만들고 값이 같은 피처에서 터지지 않는다`() {
        val train = listOf(doubleArrayOf(10.0, 7.0), doubleArrayOf(20.0, 7.0), doubleArrayOf(30.0, 7.0))
        val standardizer = Standardizer.fit(train)
        val transformed = train.map { standardizer.transform(it) }

        assertEquals(0.0, transformed.sumOf { it[0] }, 1e-12)
        assertEquals(1.0, transformed.sumOf { it[0] * it[0] } / 3, 1e-12)
        assertContentEquals(listOf(0.0, 0.0, 0.0), transformed.map { it[1] })
    }

    @Test
    fun `walk-forward 는 맞출 경기보다 앞의 경기만 학습에 넘긴다`() {
        val examples = List(6) { Example("m$it", DoubleArray(0), it.toDouble()) }
        val trainSizes = ArrayList<Int>()

        val predicted =
            walkForward(examples, minTrain = 3) { train ->
                trainSizes.add(train.size)
                // 학습 표본의 마지막 경기 번호를 돌려준다. 맞출 경기 바로 앞이어야 한다.
                val last = train.last().label
                { _ -> last }
            }

        assertEquals(listOf(3, 4, 5), trainSizes)
        assertContentEquals(doubleArrayOf(2.0, 3.0, 4.0), predicted)
    }

    @Test
    fun `bias 없이 학습하면 두 팀을 맞바꿨을 때 확률이 뒤집힌다`() {
        val random = Random(3)
        val raw = List(300) { doubleArrayOf(random.nextDouble(-1.0, 3.0)) }
        val y = DoubleArray(raw.size) { if (random.nextDouble() < sigmoid(raw[it][0])) 1.0 else 0.0 }
        val standardizer = Standardizer.fit(raw, center = false)
        val model = LogisticRegression(0.5, l2 = 0.0, epochs = 500, fitBias = false)
        model.fit(raw.map { standardizer.transform(it) }, y)

        val p = model.predict(standardizer.transform(doubleArrayOf(0.7)))
        val swapped = model.predict(standardizer.transform(doubleArrayOf(-0.7)))

        assertEquals(0.0, model.bias)
        assertEquals(1.0, p + swapped, 1e-12)
        assertEquals(0.5, model.predict(standardizer.transform(doubleArrayOf(0.0))), 1e-12)
    }
}
