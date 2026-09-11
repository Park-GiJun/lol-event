package com.gijun.main.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RatingMathTest {

    @Test
    fun `같은 점수면 기대 승률은 반반이다`() {
        assertEquals(0.5, RatingMath.expected(1500.0, 1500.0), 1e-12)
    }

    @Test
    fun `400점 차이는 약 10대 1이다`() {
        assertEquals(10.0 / 11.0, RatingMath.expected(1900.0, 1500.0), 1e-12)
    }

    @Test
    fun `기대 승률은 대칭이다`() {
        val p = RatingMath.expected(1700.0, 1450.0)
        assertEquals(1.0 - p, RatingMath.expected(1450.0, 1700.0), 1e-12)
    }

    @Test
    fun `배치 경계에서 K가 바뀐다`() {
        assertEquals(RatingMath.K_PLACEMENT, RatingMath.kFactor(RatingMath.PLACEMENT - 1))
        assertEquals(RatingMath.K_BASE, RatingMath.kFactor(RatingMath.PLACEMENT))
    }

    @Test
    fun `표시값은 표본이 없으면 시작점으로 완전히 수축한다`() {
        assertEquals(RatingMath.START, RatingMath.display(1800.0, 0), 1e-12)
    }

    @Test
    fun `배치를 막 벗어난 사람은 딱 절반만 반영된다`() {
        // n = 10, PRIOR = 10 이라 가중치가 0.5 다.
        assertEquals(1650.0, RatingMath.display(1800.0, 10), 1e-12)
    }

    @Test
    fun `표본이 쌓일수록 표시값이 원값에 붙는다`() {
        val far = RatingMath.display(1800.0, 20)
        val farther = RatingMath.display(1800.0, 200)
        assertTrue(far < farther)
        assertTrue(farther < 1800.0)
        assertEquals(1800.0, RatingMath.display(1800.0, 1_000_000), 0.1)
    }

    @Test
    fun `수축은 아래쪽에도 똑같이 걸린다`() {
        assertEquals(1350.0, RatingMath.display(1200.0, 10), 1e-12)
    }
}
