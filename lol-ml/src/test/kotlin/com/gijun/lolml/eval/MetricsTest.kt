package com.gijun.lolml.eval

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricsTest {
    private val actual = doubleArrayOf(1.0, 0.0, 1.0, 0.0)

    @Test
    fun `전부 0_5 라고 답하면 기준선 값이 나온다`() {
        val coin = DoubleArray(4) { 0.5 }
        assertEquals(0.6931, logLoss(coin, actual), 1e-4)
        assertEquals(0.25, brierScore(coin, actual), 1e-12)
    }

    @Test
    fun `자신 있게 틀리면 모른다고 한 것보다 벌점이 크다`() {
        val confidentlyWrong = doubleArrayOf(0.1, 0.9, 0.1, 0.9)
        val confidentlyRight = doubleArrayOf(0.9, 0.1, 0.9, 0.1)
        assertEquals(2.3026, logLoss(confidentlyWrong, actual), 1e-4)
        assertEquals(0.1054, logLoss(confidentlyRight, actual), 1e-4)
    }

    @Test
    fun `확률이 0 이나 1 이어도 무한대가 되지 않는다`() {
        assertTrue(logLoss(doubleArrayOf(0.0, 1.0), doubleArrayOf(1.0, 0.0)).isFinite())
    }

    @Test
    fun `캘리브레이션 표는 구간마다 예측 평균과 실제 승률을 낸다`() {
        val table = calibrationTable(doubleArrayOf(0.62, 0.68, 0.31, 1.0), doubleArrayOf(1.0, 0.0, 0.0, 1.0))
        assertEquals(listOf(1, 2, 1), table.map { it.count })
        assertEquals(0.65, table[1].meanPredicted, 1e-12)
        assertEquals(0.5, table[1].actualWinRate, 1e-12)
        assertEquals(0.9, table[2].from, 1e-12)
    }
}
