package com.gijun.lolml.feature

import com.gijun.lolml.TEAM_A
import com.gijun.lolml.TEAM_B
import com.gijun.lolml.data.Position
import com.gijun.lolml.data.Team
import com.gijun.lolml.match
import com.gijun.lolml.participant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeatureBuilderTest {
    @Test
    fun `Elo 는 점수가 같으면 반반이고 이변일수록 많이 움직인다`() {
        assertEquals(0.5, Elo.expected(1500.0, 1500.0), 1e-12)
        assertEquals(0.909, Elo.expected(1900.0, 1500.0), 1e-3)
        assertEquals(1516.0, Elo.updated(1500.0, expected = 0.5, score = 1.0), 1e-12)
        assertTrue(Elo.updated(1500.0, 0.1, 1.0) > Elo.updated(1500.0, 0.9, 1.0))
    }

    @Test
    fun `첫 경기는 아는 것이 없어 피처가 전부 0 이다`() {
        val examples = FeatureBuilder().build(listOf(match("m1", TEAM_A, TEAM_B, blueWin = true)))
        assertContentEquals(DoubleArray(FeatureBuilder.NAMES.size), examples.single().features)
        assertEquals(1.0, examples.single().label)
    }

    @Test
    fun `이긴 팀은 다음 경기에서 Elo 가 앞선다`() {
        val examples =
            FeatureBuilder().build(
                listOf(
                    match("m1", TEAM_A, TEAM_B, blueWin = true, blueKills = 9),
                    // 진영을 바꿔 앉는다. 이번에는 레드가 직전 승자다.
                    match("m2", TEAM_B, TEAM_A, blueWin = true),
                ),
            )
        val (eloDiff, kdaDiff, gamesDiff) = examples[1].features.toList()
        assertEquals(-32.0, eloDiff, 1e-9)
        assertTrue(kdaDiff < 0.0)
        assertEquals(0.0, gamesDiff, 1e-12)
    }

    /** 누수 검사. 어떤 경기의 결과를 바꿔도 그 경기까지의 피처는 그대로여야 한다. */
    @Test
    fun `마지막 경기의 승패와 KDA 를 바꿔도 그 경기의 피처는 변하지 않는다`() {
        val history =
            listOf(
                match("m1", TEAM_A, TEAM_B, blueWin = true, blueGold = 12_000),
                match("m2", TEAM_A, TEAM_B, blueWin = false, blueGold = 8_000),
            )
        val asPlayed = FeatureBuilder().build(history + match("m3", TEAM_A, TEAM_B, blueWin = true, blueKills = 20, blueGold = 15_000))
        val flipped = FeatureBuilder().build(history + match("m3", TEAM_A, TEAM_B, blueWin = false, blueKills = 0, blueGold = 5_000))

        for (i in asPlayed.indices) assertContentEquals(asPlayed[i].features, flipped[i].features)
        assertEquals(1.0, asPlayed.last().label)
        assertEquals(0.0, flipped.last().label)
    }

    @Test
    fun `최근 KDA 는 최근 N 경기만 본다`() {
        val state = PlayerState()
        assertEquals(PlayerState.DEFAULT_KDA, state.recentKda())

        val bad = match("old", TEAM_A, TEAM_B, blueWin = true, blueKills = 0).participants.first()
        val good = match("new", TEAM_A, TEAM_B, blueWin = true, blueKills = 9).participants.first()
        state.update(bad, 0.5)
        repeat(PlayerState.RECENT_WINDOW) { state.update(good, 0.5) }

        // (9 + 3) / 3 = 4. 옛 경기(1.0)는 창 밖으로 밀려났다.
        assertEquals(4.0, state.recentKda(), 1e-12)
        assertEquals(PlayerState.RECENT_WINDOW + 1, state.games)
    }

    @Test
    fun `자리 피처는 그 자리에서 한 비율과 기대보다 더 이긴 정도를 본다`() {
        val state = PlayerState()
        val topWin = participant("p", Team.BLUE, win = true, position = Position.TOP)
        val midLoss = participant("p", Team.BLUE, win = false, position = Position.MID)
        repeat(3) { state.update(topWin, teamExpected = 0.5) }
        state.update(midLoss, teamExpected = 0.5)

        assertEquals(0.75, state.seatShare(Position.TOP), 1e-12)
        assertEquals(0.0, state.seatShare(Position.SUPPORT), 1e-12)
        // 세 번 다 이겨 기대보다 1.5 승 많다. 표본이 3 뿐이라 0.5 가 아니라 1.5 / (3 + 10) 으로 줄여 본다.
        assertEquals(1.5 / 13.0, state.seatEdge(Position.TOP), 1e-12)
        assertTrue(state.seatEdge(Position.MID) < 0.0)
        assertEquals(0.0, state.seatEdge(Position.SUPPORT), 1e-12)
    }

    @Test
    fun `자리를 바꿔 앉으면 익숙한 자리에 앉은 팀이 자리 피처에서 앞선다`() {
        val examples =
            FeatureBuilder().build(
                listOf(
                    match("m1", TEAM_A, TEAM_B, blueWin = true),
                    // 블루는 순서를 뒤집어 처음 앉는 자리로 간다(미드만 그대로). 레드는 그대로.
                    match("m2", TEAM_A.reversed(), TEAM_B, blueWin = true),
                ),
            )
        val seatShareDiff = examples[1].features[FeatureBuilder.NAMES.indexOf("seatShareDiff")]
        val seatEdgeDiff = examples[1].features[FeatureBuilder.NAMES.indexOf("seatEdgeDiff")]

        assertEquals(0.2 - 1.0, seatShareDiff, 1e-12)
        // 블루는 미드 한 명만 +0.5/11, 레드는 다섯 모두 −0.5/11.
        assertEquals((0.5 / 11.0) / 5 + 0.5 / 11.0, seatEdgeDiff, 1e-12)
    }

    @Test
    fun `라인 맞대결은 같은 자리의 두 사람 중 더 큰 쪽이 이긴다`() {
        val duels = LaneDuels.of(match("m1", TEAM_A, TEAM_B, blueWin = false, blueGold = 12_000))

        assertEquals(5, duels.size)
        assertTrue(duels.all { it.winner.team == Team.BLUE && it.winner.position == it.loser.position })
        // 점수가 같으면 누가 이겼는지 말할 수 없다.
        assertEquals(0, LaneDuels.of(match("m2", TEAM_A, TEAM_B, blueWin = true)).size)
    }

    @Test
    fun `팀은 졌어도 라인을 이긴 쪽이 라인 피처에서 앞선다`() {
        val examples =
            FeatureBuilder().build(
                listOf(
                    match("m1", TEAM_A, TEAM_B, blueWin = false, blueGold = 12_000),
                    match("m2", TEAM_A, TEAM_B, blueWin = true),
                ),
            )
        val second = FeatureBuilder.NAMES.zip(examples[1].features.toList()).toMap()

        // 라인 1승 0패 vs 0승 1패: (1 + 5) / 11 − (0 + 5) / 11
        assertEquals(1.0 / 11.0, second.getValue("laneWinRateDiff"), 1e-12)
        assertTrue(second.getValue("seatLaneWinRateDiff") > second.getValue("laneWinRateDiff"))
        // 배치 구간 K 26 × (1 − 0.5) = 13 씩 주고받는다.
        assertEquals(26.0, second.getValue("laneEloDiff"), 1e-9)
        // 팀 승패로 센 것은 반대 방향이다.
        assertTrue(second.getValue("winRateDiff") < 0.0)
        assertTrue(second.getValue("eloDiff") < 0.0)
    }

    @Test
    fun `자리 Elo 는 라인 Elo 에 그 자리의 잔차를 표본만큼만 얹는다`() {
        val state = PlayerState()
        assertEquals(Elo.INITIAL, state.seatElo(Position.TOP, offRolePrior = -0.1), 1e-12)

        // 탑에서 반반으로 본 맞대결을 네 번 다 이겼다. 잔차 합 +2.
        repeat(4) { state.updateLane(Position.TOP, won = true, expected = 0.5, newLaneElo = 1550.0) }

        // 주 포지션: 1550 + 695 × 2 / (4 + 80)
        assertEquals(1550.0 + PlayerState.ELO_PER_PROBABILITY * 2.0 / 84.0, state.seatElo(Position.TOP, offRolePrior = -0.1), 1e-9)
        // 안 가 본 자리: 비주력 기본값(−0.1) 쪽으로 당겨져 라인 Elo 보다 낮다. 1550 + 695 × (80 × −0.1) / 80
        assertEquals(1550.0 - PlayerState.ELO_PER_PROBABILITY * 0.1, state.seatElo(Position.MID, offRolePrior = -0.1), 1e-9)
        assertEquals(0 to 0.0, state.offRoleTotals())
    }
}
