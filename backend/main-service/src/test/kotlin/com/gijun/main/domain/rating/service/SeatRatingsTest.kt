package com.gijun.main.domain.rating.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeatRatingsTest {
    @Test
    fun `기록이 없는 사람은 어느 자리든 전체 Elo 그대로다`() {
        val seats = SeatRatings()

        assertEquals(1500.0, seats.seatElo("손님#KR1", "TOP", 1500.0))
    }

    @Test
    fun `기대만큼 이긴 자리는 전체 Elo 와 같다`() {
        val seats = SeatRatings()
        repeat(10) { seats.add("a", "TOP", expected = 0.5, won = it % 2 == 0) }

        assertEquals(1600.0, seats.seatElo("a", "TOP", 1600.0), 1e-9)
    }

    @Test
    fun `기대보다 못 이긴 자리는 깎이고 표본이 적을수록 덜 깎인다`() {
        // c 의 비주력 기록이 기본값을 0 가까이 잡아 준다. 없으면 기본값이 a 의 원딜 승률 그 자체가 된다.
        fun seeded() =
            SeatRatings().also { seats ->
                repeat(20) { seats.add("c", "MID", 0.5, won = it % 2 == 0) }
                repeat(100) { seats.add("c", "SUPPORT", 0.5, won = it % 2 == 0) }
            }

        val few = seeded()
        repeat(20) { few.add("a", "TOP", 0.5, won = it % 2 == 0) }
        repeat(4) { few.add("a", "ADC", 0.5, won = false) }

        val many = seeded()
        repeat(40) { many.add("a", "TOP", 0.5, won = it % 2 == 0) }
        repeat(20) { many.add("a", "ADC", 0.5, won = false) }

        val fewAdc = few.seatElo("a", "ADC", 1500.0)
        val manyAdc = many.seatElo("a", "ADC", 1500.0)
        assertTrue(fewAdc < 1500.0) { "원딜에서 내리 졌으면 깎여야 한다: $fewAdc" }
        assertTrue(manyAdc < fewAdc) { "같은 승률이면 표본이 많은 쪽이 더 깎인다: $manyAdc vs $fewAdc" }
        assertEquals(1500.0, few.seatElo("a", "TOP", 1500.0), 1e-9)
    }

    @Test
    fun `한 번도 안 가 본 자리는 남들이 비주력 자리에서 밑돈 만큼 깎고 시작한다`() {
        val seats = SeatRatings()
        // b 는 미드가 주 포지션이고, 비주력인 서포터에서 기대보다 못 했다.
        repeat(20) { seats.add("b", "MID", 0.5, won = it % 2 == 0) }
        repeat(10) { seats.add("b", "SUPPORT", 0.5, won = it < 3) }
        // a 는 탑만 했다.
        repeat(20) { seats.add("a", "TOP", 0.5, won = it % 2 == 0) }

        val prior = seats.offRolePrior()
        assertEquals(-0.2, prior, 1e-9)
        // 표본 0 이면 기본값 그대로다: 695 × (-0.2).
        assertEquals(1500.0 + SeatRatings.ELO_PER_PROBABILITY * prior, seats.seatElo("a", "JUNGLE", 1500.0), 1e-9)
        assertEquals(1500.0, seats.seatElo("a", "TOP", 1500.0), 1e-9)
    }

    @Test
    fun `비주력 자리가 평균적으로 더 잘했어도 가산점은 주지 않는다`() {
        val seats = SeatRatings()
        repeat(20) { seats.add("b", "MID", 0.5, won = it % 2 == 0) }
        repeat(10) { seats.add("b", "SUPPORT", 0.5, won = true) }
        repeat(20) { seats.add("a", "TOP", 0.5, won = it % 2 == 0) }

        assertEquals(0.0, seats.offRolePrior())
        assertEquals(1500.0, seats.seatElo("a", "JUNGLE", 1500.0), 1e-9)
    }

    @Test
    fun `옛 포지션 표기도 다섯 자리로 맞춘다`() {
        assertEquals("ADC", SeatRatings.normalize("bottom"))
        assertEquals("MID", SeatRatings.normalize("MIDDLE"))
        assertEquals("SUPPORT", SeatRatings.normalize("UTILITY"))
        assertEquals(null, SeatRatings.normalize("UNKNOWN"))
        assertEquals(null, SeatRatings.normalize(""))
    }
}
