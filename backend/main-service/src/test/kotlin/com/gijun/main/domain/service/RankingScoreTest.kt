package com.gijun.main.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RankingScoreTest {

    @Test
    fun `경기가 없으면 사전 평균을 그대로 돌려준다`() {
        assertEquals(50.0, RankingScore.shrunkWinRate(wins = 0, games = 0))
    }

    @Test
    fun `표본이 작을수록 평균 쪽으로 크게 당겨진다`() {
        // 둘 다 승률 100% 지만 경기 수가 다르다
        val onePerfect = RankingScore.shrunkWinRate(wins = 1, games = 1)
        val tenPerfect = RankingScore.shrunkWinRate(wins = 10, games = 10)

        assertTrue(onePerfect < tenPerfect) { "1경기 100% 가 10경기 100% 보다 높게 나오면 안 된다" }
        assertTrue(onePerfect < 60.0) { "1경기 100% 는 50% 근처로 내려와야 한다, 실제=$onePerfect" }
    }

    @Test
    fun `표본이 커지면 관측값에 수렴한다`() {
        val many = RankingScore.shrunkWinRate(wins = 700, games = 1000)
        assertTrue(many > 69.0) { "표본이 크면 관측 70% 에 가까워야 한다, 실제=$many" }
    }

    @Test
    fun `보정은 평균을 넘어가지 않는다`() {
        // 전패도 0% 가 아니라 50% 쪽으로 올라온다
        val allLoss = RankingScore.shrunkWinRate(wins = 0, games = 2)
        assertTrue(allLoss in 0.0..50.0)
        assertTrue(allLoss > 30.0) { "2경기 전패가 0% 로 표시되면 안 된다, 실제=$allLoss" }
    }

    @Test
    fun `shrinkToward 는 평균 기준 지표에도 같은 방식으로 동작한다`() {
        // 정규화된 KDA·데미지는 평균이 1.0 이다
        val noisy = RankingScore.shrinkToward(observed = 3.0, mean = 1.0, games = 1)
        val solid = RankingScore.shrinkToward(observed = 3.0, mean = 1.0, games = 100)

        assertTrue(noisy < solid)
        assertTrue(noisy < 1.3) { "1경기짜리 KDA 3배는 거의 평균으로 눌려야 한다, 실제=$noisy" }
        assertTrue(solid > 2.7) { "100경기짜리는 관측값을 존중해야 한다, 실제=$solid" }
    }

    @Test
    fun `표본 등급은 경기 수 구간을 따른다`() {
        assertEquals("INSUFFICIENT", RankingScore.sampleGrade(4))
        assertEquals("LOW", RankingScore.sampleGrade(5))
        assertEquals("MEDIUM", RankingScore.sampleGrade(10))
        assertEquals("HIGH", RankingScore.sampleGrade(30))
    }
}
