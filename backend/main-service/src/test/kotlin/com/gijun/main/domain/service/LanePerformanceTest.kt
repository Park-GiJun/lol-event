package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LanePerformanceTest {

    private val positions = listOf("TOP", "JUNGLE", "MID", "ADC", "SUPPORT")

    /**
     * 모든 지표가 [scale] 에 정비례하는 참가자. scale 2 는 scale 1 상대를 모든 면에서 두 배로 앞선다.
     * deaths 만 고정해 KDA 도 같은 비율로 벌어지게 한다.
     */
    private fun player(riotId: String, teamId: Int, position: String, scale: Int) = MatchParticipant(
        riotId = riotId,
        champion = "Champ",
        team = if (teamId == 100) "BLUE" else "RED",
        teamId = teamId,
        win = teamId == 100,
        assignedPosition = position,
        kills = 2 * scale, deaths = 2, assists = 4 * scale,
        damage = 10_000 * scale, cs = 100 * scale, gold = 10_000 * scale,
        visionScore = 20 * scale, damageDealtToObjectives = 5_000 * scale,
        totalDamageTaken = 20_000 * scale,
    )

    private fun team(teamId: Int, scaleOf: (String) -> Int) =
        positions.map { player("$teamId-$it", teamId, it, scaleOf(it)) }

    @Test
    fun `같은 정도로 앞서면 포지션이 달라도 같은 점수가 나온다`() {
        // 모든 라인이 상대를 똑같이 2배로 앞선 경우. 서포터의 딜량이 원딜의 1/6이어도
        // 비교 대상이 상대 서포터이므로 점수는 원딜과 같아야 한다.
        val scores = LanePerformance.scores(team(100) { 2 }, team(200) { 1 })
        val blue = positions.map { scores.getValue("100-$it") }

        blue.forEach { assertEquals(2.0 / 3.0, it, 1e-9) }
        assertEquals(1, blue.map { "%.9f".format(it) }.distinct().size) {
            "포지션마다 점수가 갈리면 안 된다: $blue"
        }
    }

    @Test
    fun `호각이면 정확히 중립값이 나온다`() {
        val scores = LanePerformance.scores(team(100) { 1 }, team(200) { 1 })
        scores.values.forEach { assertEquals(LanePerformance.NEUTRAL, it, 1e-9) }
    }

    @Test
    fun `서포터가 원딜보다 딜량이 낮다고 손해 보지 않는다`() {
        // 실제 배치에 가깝게 — 원딜은 딜량이 크고 서포터는 시야가 크다. 양 팀이 대칭이므로 전원 호각이어야 한다.
        val blue = listOf(
            player("b-TOP", 100, "TOP", 1),
            player("b-JUNGLE", 100, "JUNGLE", 1),
            player("b-MID", 100, "MID", 1),
            player("b-ADC", 100, "ADC", 1).copy(damage = 60_000, visionScore = 10),
            player("b-SUPPORT", 100, "SUPPORT", 1).copy(damage = 8_000, visionScore = 80),
        )
        val red = blue.map { it.copy(riotId = "r-${it.assignedPosition}", teamId = 200, team = "RED", win = false) }

        val scores = LanePerformance.scores(blue, red)
        assertEquals(scores.getValue("b-ADC"), scores.getValue("b-SUPPORT"), 1e-9)
        scores.values.forEach { assertEquals(0.5, it, 1e-9) }
    }

    @Test
    fun `상대 라이너를 특정할 수 없으면 판단을 보류한다`() {
        // 한쪽 팀이 2명뿐이라 다섯 라인 중 셋은 비교 상대가 없다.
        val blue = team(100) { 2 }
        val red = listOf(
            player("r-TOP", 200, "TOP", 1),
            player("r-MID", 200, "MID", 1),
        )
        val blueScores = LanePerformance.scores(blue, red)
            .filterKeys { it.startsWith("100-") }.values

        assertEquals(3, blueScores.count { it == LanePerformance.NEUTRAL }) {
            "상대를 못 찾은 세 명은 보류돼야 한다: $blueScores"
        }
        blueScores.filter { it != LanePerformance.NEUTRAL }.forEach {
            assertTrue(it > 0.5) { "두 배로 앞섰는데 점수가 0.5 이하다: $it" }
        }
    }

    @Test
    fun `지표가 전부 0 이어도 중립으로 떨어질 뿐 터지지 않는다`() {
        val blue = positions.map { MatchParticipant(riotId = "b-$it", champion = "C", team = "BLUE", teamId = 100, win = true, assignedPosition = it) }
        val red  = positions.map { MatchParticipant(riotId = "r-$it", champion = "C", team = "RED",  teamId = 200, win = false, assignedPosition = it) }

        LanePerformance.scores(blue, red).values.forEach { assertEquals(0.5, it, 1e-9) }
    }

    @Test
    fun `모든 포지션 가중치의 합은 1 이다`() {
        // 가중치 합이 1이 아니면 포지션마다 기준선이 0.5에서 어긋난다.
        val me = player("me", 100, "TOP", 1)
        val opp = player("opp", 200, "TOP", 1)
        Position.entries.filter { it != Position.UNKNOWN }.forEach { pos ->
            assertEquals(0.5, LanePerformance.laneScore(me, opp, pos), 1e-9) {
                "$pos 의 가중치 합이 1이 아니다"
            }
        }
    }
}
