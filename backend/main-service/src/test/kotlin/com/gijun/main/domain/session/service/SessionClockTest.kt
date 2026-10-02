package com.gijun.main.domain.session.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 세션 경계. 실측으로 확인한 경기 시작 시각 분포가 21시 43판 / 22시 87판 / 23시 54판 /
 * **00시 14판** 이라, 자정 기준에서는 그 14판이 다음 날로 떨어져 있었다.
 */
class SessionClockTest {
    /** KST 로 이 시각인 경기의 epoch ms. */
    private fun kst(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, SessionClock.ZONE).toInstant().toEpochMilli()

    @Test
    fun `저녁 경기는 그날 세션이다`() {
        assertEquals(LocalDate.of(2026, 9, 14), SessionClock.sessionDate(kst(2026, 9, 14, 21, 30)))
    }

    @Test
    fun `자정을 넘긴 경기는 전날 세션이다`() {
        // 23:40 에 시작한 판과 00:20 에 끝난 판이 갈리지 않게 하는 것이 이 함수의 목적이다.
        assertEquals(LocalDate.of(2026, 9, 14), SessionClock.sessionDate(kst(2026, 9, 15, 0, 20)))
        assertEquals(LocalDate.of(2026, 9, 14), SessionClock.sessionDate(kst(2026, 9, 15, 2, 0)))
    }

    @Test
    fun `오전 다섯 시 오십구 분은 아직 전날이다`() {
        assertEquals(LocalDate.of(2026, 9, 14), SessionClock.sessionDate(kst(2026, 9, 15, 5, 59)))
    }

    @Test
    fun `오전 여섯 시 정각부터 새 세션이다`() {
        assertEquals(LocalDate.of(2026, 9, 15), SessionClock.sessionDate(kst(2026, 9, 15, 6, 0)))
    }

    @Test
    fun `세션 구간은 오전 여섯 시에 시작해 다음날 여섯 시에 끝난다`() {
        val (from, until) = SessionClock.rangeMs(LocalDate.of(2026, 9, 14))

        assertEquals(kst(2026, 9, 14, 6, 0), from)
        assertEquals(kst(2026, 9, 15, 6, 0), until)
    }

    @Test
    fun `구간의 끝은 열려 있어 다음 세션 첫 경기를 포함하지 않는다`() {
        val (from, until) = SessionClock.rangeMs(LocalDate.of(2026, 9, 14))
        val nextSessionFirstGame = kst(2026, 9, 15, 6, 0)

        assertTrue(nextSessionFirstGame !in from until until, "닫으면 그 경기가 양쪽 세션에 들어간다")
        assertEquals(LocalDate.of(2026, 9, 15), SessionClock.sessionDate(nextSessionFirstGame))
    }

    @Test
    fun `구간과 날짜 판정은 서로 맞아떨어진다`() {
        val date = LocalDate.of(2026, 9, 14)
        val (from, until) = SessionClock.rangeMs(date)

        for (ms in listOf(from, from + 1, kst(2026, 9, 15, 3, 0), until - 1)) {
            assertEquals(date, SessionClock.sessionDate(ms), "구간 안이면 그 세션이어야 한다: $ms")
        }
    }
}
