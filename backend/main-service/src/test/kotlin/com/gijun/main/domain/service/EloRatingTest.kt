package com.gijun.main.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class EloRatingTest {

    /** 표본이 충분히 쌓여 K 가 최소값(24)으로 잠긴 플레이어. */
    private fun veteran(rating: Double) = EloRating.Rated(rating, games = 50)

    /** 배치 중. K 가 가장 큰 구간(64)에 있다. */
    private fun rookie(rating: Double = EloRating.INITIAL) = EloRating.Rated(rating, games = 0)

    private fun team(rating: Double, n: Int = 5) = List(n) { veteran(rating) }

    private fun total(d: EloRating.Deltas) = d.teamA.sum() + d.teamB.sum()

    @Test
    fun `실력이 같은 5대5 는 표준 Elo 와 같은 값을 낸다`() {
        val d = EloRating.deltas(team(1000.0), team(1000.0), aWon = true)

        // 기대 승률 0.5, K 24 → 24 * (1 - 0.5) = 12
        d.teamA.forEach { assertEquals(12.0, it, 1e-9) }
        d.teamB.forEach { assertEquals(-12.0, it, 1e-9) }
    }

    @Test
    fun `한 경기에서 오간 점수의 총합은 항상 0 이다`() {
        val cases = listOf(
            EloRating.deltas(team(1000.0), team(1000.0), aWon = true),
            EloRating.deltas(team(1300.0), team(880.0), aWon = false),
            EloRating.deltas(
                listOf(rookie(), veteran(1250.0), veteran(970.0), rookie(1100.0), veteran(1040.0)),
                listOf(veteran(1180.0), rookie(), rookie(900.0), veteran(1010.0), veteran(1330.0)),
                aWon = true,
            ),
        )
        cases.forEachIndexed { i, d ->
            assertTrue(abs(total(d)) < 1e-9) { "case $i 의 총합이 0 이 아니다: ${total(d)}" }
        }
    }

    @Test
    fun `팀 인원이 어긋나도 총합은 0 으로 남는다`() {
        // 4대5. 예전 구현은 여기서 인원 차이만큼 점수가 새거나 사라졌다.
        val d = EloRating.deltas(team(1000.0, n = 4), team(1050.0, n = 5), aWon = true)
        assertTrue(abs(total(d)) < 1e-9) { "4대5 총합이 0 이 아니다: ${total(d)}" }
    }

    @Test
    fun `배치 중인 사람이 같은 팀 베테랑보다 크게 움직인다`() {
        val d = EloRating.deltas(
            listOf(rookie(), veteran(1000.0), veteran(1000.0), veteran(1000.0), veteran(1000.0)),
            team(1000.0),
            aWon = true,
        )
        val rookieDelta = d.teamA[0]
        val veteranDelta = d.teamA[1]

        assertTrue(rookieDelta > veteranDelta) {
            "배치 중($rookieDelta)이 베테랑($veteranDelta)보다 크게 움직여야 한다"
        }
        // K 64 대 24 → 비율이 그대로 유지돼야 한다
        assertEquals(64.0 / 24.0, rookieDelta / veteranDelta, 1e-9)
        assertTrue(abs(total(d)) < 1e-9)
    }

    @Test
    fun `약팀이 이기면 더 많이 얻고 강팀이 이기면 조금만 얻는다`() {
        val underdogWins = EloRating.deltas(team(900.0), team(1200.0), aWon = true).teamA[0]
        val favoriteWins = EloRating.deltas(team(1200.0), team(900.0), aWon = true).teamA[0]

        assertTrue(underdogWins > favoriteWins) {
            "이변 보상은 (S - E) 항만으로 나와야 한다: 약팀=$underdogWins, 강팀=$favoriteWins"
        }
        // 별도 이변 배율이 없으므로 K 상한을 넘을 수 없다
        assertTrue(underdogWins < EloRating.kFactor(50)) {
            "변동폭이 K(${EloRating.kFactor(50)})를 넘었다: $underdogWins"
        }
    }

    @Test
    fun `이기고 잃거나 지고 얻는 경우는 없다`() {
        val won = EloRating.deltas(team(700.0), team(1400.0), aWon = true)
        won.teamA.forEach { assertTrue(it > 0) { "이겼는데 점수가 줄었다: $it" } }
        won.teamB.forEach { assertTrue(it < 0) { "졌는데 점수가 늘었다: $it" } }

        val lost = EloRating.deltas(team(1400.0), team(700.0), aWon = false)
        lost.teamA.forEach { assertTrue(it < 0) { "졌는데 점수가 늘었다: $it" } }
        lost.teamB.forEach { assertTrue(it > 0) { "이겼는데 점수가 줄었다: $it" } }
    }

    @Test
    fun `기대 승률은 두 팀 관점에서 합이 1 이다`() {
        val a = EloRating.expectedScore(1150.0, 980.0)
        val b = EloRating.expectedScore(980.0, 1150.0)
        assertEquals(1.0, a + b, 1e-9)
        assertTrue(a > 0.5)
        assertEquals(0.5, EloRating.expectedScore(1000.0, 1000.0), 1e-9)
    }

    @Test
    fun `K 는 경기 수가 쌓일수록 작아진다`() {
        val ks = listOf(0, 4, 5, 14, 15, 92).map { EloRating.kFactor(it) }
        assertEquals(listOf(64.0, 64.0, 32.0, 32.0, 24.0, 24.0), ks)
    }

    // ────────── 개인 성적 반영 ──────────

    private fun perf(rating: Double, lane: Double) =
        EloRating.Rated(rating, games = 50, lanePerformance = lane)

    /** 라인전 점수만 다르고 나머지는 동일한 5대5. A팀 0번이 잘했고 1번이 못했다. */
    private fun mixedPerformance(aWon: Boolean) = EloRating.deltas(
        listOf(perf(1000.0, 0.70), perf(1000.0, 0.30), perf(1000.0, 0.50),
               perf(1000.0, 0.50), perf(1000.0, 0.50)),
        List(5) { perf(1000.0, 0.50) },
        aWon = aWon,
    )

    @Test
    fun `성적이 갈려도 총합은 0 그대로다`() {
        assertTrue(abs(total(mixedPerformance(aWon = true))) < 1e-9)
        assertTrue(abs(total(mixedPerformance(aWon = false))) < 1e-9)
    }

    @Test
    fun `이긴 팀에서는 잘한 사람이 더 얻는다`() {
        val d = mixedPerformance(aWon = true).teamA
        assertTrue(d[0] > d[2]) { "잘한 사람(${d[0]})이 평범한 사람(${d[2]})보다 더 얻어야 한다" }
        assertTrue(d[2] > d[1]) { "평범한 사람(${d[2]})이 못한 사람(${d[1]})보다 더 얻어야 한다" }
    }

    @Test
    fun `진 팀에서는 잘한 사람이 덜 잃는다`() {
        val d = mixedPerformance(aWon = false).teamA
        d.forEach { assertTrue(it < 0) { "졌는데 점수가 올랐다: $it" } }
        assertTrue(d[0] > d[2]) { "잘한 사람(${d[0]})이 평범한 사람(${d[2]})보다 덜 잃어야 한다" }
        assertTrue(d[2] > d[1]) { "평범한 사람(${d[2]})이 못한 사람(${d[1]})보다 덜 잃어야 한다" }
    }

    @Test
    fun `아무리 잘해도 지면 잃고 아무리 못해도 이기면 얻는다`() {
        // 예전 구현은 성적을 가산항으로 더해서 이 성질이 깨져 있었다.
        val ace = perf(1000.0, 1.0)
        val thrower = perf(1000.0, 0.0)
        val team = listOf(ace, thrower, perf(1000.0, 0.5), perf(1000.0, 0.5), perf(1000.0, 0.5))

        EloRating.deltas(team, List(5) { perf(1000.0, 0.5) }, aWon = false).teamA
            .forEach { assertTrue(it < 0) { "패배 팀인데 점수가 올랐다: $it" } }
        EloRating.deltas(team, List(5) { perf(1000.0, 0.5) }, aWon = true).teamA
            .forEach { assertTrue(it > 0) { "승리 팀인데 점수가 내렸다: $it" } }
    }

    @Test
    fun `팀 전체가 똑같이 잘해도 배분은 균등하다`() {
        // 이긴 팀은 원래 다섯 명 다 라인전 점수가 높게 나온다. 그 공통분은 몫을 가를 근거가 못 된다.
        val d = EloRating.deltas(
            List(5) { perf(1000.0, 0.75) },
            List(5) { perf(1000.0, 0.25) },
            aWon = true,
        )
        d.teamA.forEach { assertEquals(12.0, it, 1e-9) }
        d.teamB.forEach { assertEquals(-12.0, it, 1e-9) }
    }

    @Test
    fun `성적 차이가 벌어져도 배분 폭에는 상한이 있다`() {
        val d = mixedPerformance(aWon = true).teamA
        val extreme = EloRating.deltas(
            listOf(perf(1000.0, 1.0), perf(1000.0, 0.0), perf(1000.0, 0.5),
                   perf(1000.0, 0.5), perf(1000.0, 0.5)),
            List(5) { perf(1000.0, 0.5) },
            aWon = true,
        ).teamA

        // 0.70/0.30 에서 이미 상한(±0.15)에 닿으므로 더 벌려도 같은 값이어야 한다
        assertEquals(d[0], extreme[0], 1e-9)
        assertEquals(d[1], extreme[1], 1e-9)
        // 최대 1.3배 대 0.7배
        assertEquals(1.3 / 0.7, extreme[0] / extreme[1], 1e-9)
    }
}
