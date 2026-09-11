package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.model.rating.LaneResult
import com.gijun.main.domain.model.rating.PlayerRating
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class RatingEngineTest {

    private val positions = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

    private fun player(riotId: String, pid: Int, teamId: Int, pos: Position, win: Boolean) =
        MatchParticipant(
            participantId = pid,
            riotId = riotId,
            champion = "Aatrox",
            team = if (teamId == 100) "blue" else "red",
            teamId = teamId,
            win = win,
            assignedPosition = pos.name,
        )

    private fun match(
        blueWins: Boolean = true,
        gameDuration: Int = 1_800,
        participants: List<MatchParticipant>? = null,
        queueId: Int = 3130,
    ): Match {
        val roster = participants ?: (
            positions.mapIndexed { i, pos -> player("blue$i#KR1", i + 1, 100, pos, blueWins) } +
                positions.mapIndexed { i, pos -> player("red$i#KR1", i + 6, 200, pos, !blueWins) }
            )
        return Match(
            matchId = "KR_1",
            queueId = queueId,
            gameCreation = 1_700_000_000_000,
            gameDuration = gameDuration,
            participants = roster.toMutableList(),
        )
    }

    /** 블루 전원이 라인에서 이긴 점수표. */
    private fun blueWinsEveryLane(m: Match) = LaneScores.Scored(
        method = LaneMethod.LEGACY_FINAL,
        scores = m.participants.associate { it.riotId to if (it.teamId == 100) 2.0 else 1.0 },
    )

    private fun identity(riotId: String) = riotId

    // ────────── 재생 대상 필터 ──────────

    @Test
    fun `열 명이 아니거나 600초 이하면 재생하지 않는다`() {
        assertTrue(RatingEngine.isRatable(match()))
        assertFalse(RatingEngine.isRatable(match(gameDuration = 600)))
        assertFalse(RatingEngine.isRatable(match(participants = match().participants.drop(1))))
    }

    @Test
    fun `칼바람도 재생 대상이다 — 라인만 안 잡힐 뿐이다`() {
        // 명세의 필터는 큐를 보지 않는다. 내전은 내전이라 팀 레이팅에는 들어가고,
        // 라인 맞대결은 LaneScores 가 걸러 0쌍이 된다.
        val aram = match(queueId = LaneScores.ARAM_QUEUE_ID)
        assertTrue(RatingEngine.isRatable(aram))

        val outcome = RatingEngine.rate(aram, blueWinsEveryLane(aram), emptyMap(), ::identity)!!
        assertEquals(0, outcome.duelCount)
        assertTrue(outcome.ratings.all { it.laneDuels == 0 })
        assertTrue(outcome.ratings.all { it.teamGames == 1 })
    }

    // ────────── 라인 Elo ──────────

    @Test
    fun `라인 맞대결은 경기당 다섯 번 일어난다`() {
        val m = match()
        val outcome = RatingEngine.rate(m, blueWinsEveryLane(m), emptyMap(), ::identity)!!

        assertEquals(5, outcome.duelCount)
        assertTrue(outcome.ratings.all { it.laneDuels == 1 })
    }

    @Test
    fun `쌍 안에서 주고받은 점수의 합은 0이다`() {
        val m = match()
        val outcome = RatingEngine.rate(m, blueWinsEveryLane(m), emptyMap(), ::identity)!!

        val total = outcome.ratings.sumOf { it.laneElo - RatingMath.START }
        assertTrue(abs(total) < 1e-9, "라인 Elo 총합이 0이 아니다: $total")
    }

    @Test
    fun `배치 구간에서는 K가 크다`() {
        val m = match()
        val fresh = RatingEngine.rate(m, blueWinsEveryLane(m), emptyMap(), ::identity)!!
        val freshDelta = fresh.ratings.first { it.riotId == "blue0#KR1" }.laneElo - RatingMath.START

        // 양쪽 다 배치를 벗어난 상태로 같은 경기를 돌린다.
        val seasoned = m.participants.associate {
            it.riotId to PlayerRating(riotId = it.riotId, laneDuels = 50, teamGames = 50)
        }
        val settled = RatingEngine.rate(m, blueWinsEveryLane(m), seasoned, ::identity)!!
        val settledDelta = settled.ratings.first { it.riotId == "blue0#KR1" }.laneElo - RatingMath.START

        assertEquals(RatingMath.K_PLACEMENT * 0.5, freshDelta, 1e-9)
        assertEquals(RatingMath.K_BASE * 0.5, settledDelta, 1e-9)
    }

    @Test
    fun `K는 둘 중 표본이 적은 쪽에 맞춘다`() {
        // 한쪽만 배치를 벗어났으면 아직 배치 K 를 쓴다. 그래야 쌍 안에서 합이 0으로 남는다.
        val m = match()
        val mixed = mapOf("blue0#KR1" to PlayerRating(riotId = "blue0#KR1", laneDuels = 99))
        val outcome = RatingEngine.rate(m, blueWinsEveryLane(m), mixed, ::identity)!!

        val a = outcome.ratings.first { it.riotId == "blue0#KR1" }.laneElo - RatingMath.START
        val b = outcome.ratings.first { it.riotId == "red0#KR1" }.laneElo - RatingMath.START
        assertEquals(RatingMath.K_PLACEMENT * 0.5, a, 1e-9)
        assertTrue(abs(a + b) < 1e-9)
    }

    @Test
    fun `라인 점수가 같으면 갱신을 건너뛴다`() {
        // 무승부를 반반으로 나누면 "정보 없음"을 "호각"으로 바꿔 쓰는 셈이라 표본만 늘고 신호는 없다.
        val m = match()
        val tied = LaneScores.Scored(LaneMethod.LEGACY_FINAL, m.participants.associate { it.riotId to 1.0 })
        val outcome = RatingEngine.rate(m, tied, emptyMap(), ::identity)!!

        assertEquals(0, outcome.duelCount)
        assertTrue(outcome.ratings.all { it.laneDuels == 0 && it.laneElo == RatingMath.START })
        // 그래도 팀 레이팅은 움직인다.
        assertTrue(outcome.ratings.all { it.teamGames == 1 })
    }

    @Test
    fun `라인에서 이기고 팀이 지는 경우가 성립한다`() {
        // 두 레이팅이 독립이라는 것의 실제 모습이다.
        val m = match(blueWins = false)
        val outcome = RatingEngine.rate(m, blueWinsEveryLane(m), emptyMap(), ::identity)!!

        val blue = outcome.ratings.first { it.riotId == "blue0#KR1" }
        assertTrue(blue.laneElo > RatingMath.START, "라인은 이겼는데 laneElo 가 오르지 않았다")
        assertTrue(blue.teamElo < RatingMath.START, "팀은 졌는데 teamElo 가 내리지 않았다")
    }

    // ────────── 팀 Elo ──────────

    @Test
    fun `팀 레이팅은 경기당 한 번, 팀 전원이 같은 방향으로 움직인다`() {
        val m = match()
        val outcome = RatingEngine.rate(m, blueWinsEveryLane(m), emptyMap(), ::identity)!!

        val blues = outcome.ratings.filter { it.riotId.startsWith("blue") }
        assertEquals(1, blues.map { it.teamElo }.distinct().size)
        assertTrue(blues.all { it.teamElo > RatingMath.START })
        assertTrue(outcome.ratings.filter { it.riotId.startsWith("red") }.all { it.teamElo < RatingMath.START })
    }

    @Test
    fun `팀 레이팅 계산에 라인 레이팅이 섞이지 않는다`() {
        // 라인 레이팅만 크게 벌려 둔다. 팀 기대 승률은 여기에 전혀 반응하면 안 된다.
        val m = match()
        val skewed = m.participants.associate {
            it.riotId to PlayerRating(
                riotId = it.riotId,
                laneElo = if (it.teamId == 100) 2_500.0 else 500.0,
            )
        }
        val skewedOutcome = RatingEngine.rate(m, blueWinsEveryLane(m), skewed, ::identity)!!
        val flatOutcome = RatingEngine.rate(m, blueWinsEveryLane(m), emptyMap(), ::identity)!!

        assertEquals(
            flatOutcome.ratings.first { it.riotId == "blue0#KR1" }.teamElo,
            skewedOutcome.ratings.first { it.riotId == "blue0#KR1" }.teamElo,
            1e-9,
        )
    }

    // ────────── 계정 병합 ──────────

    @Test
    fun `별명은 정규 이름으로 합쳐져 한 줄로 쌓인다`() {
        val normalizer = RiotIdNormalizer(mapOf("달렸노#KR1" to "qkzxfh#KR1"))
        val roster = (
            positions.mapIndexed { i, pos ->
                player(if (i == 0) "달렸노#KR1" else "blue$i#KR1", i + 1, 100, pos, true)
            } + positions.mapIndexed { i, pos -> player("red$i#KR1", i + 6, 200, pos, false) }
            )
        val m = match(participants = roster)
        val outcome = RatingEngine.rate(m, blueWinsEveryLane(m), emptyMap(), normalizer::canonical)!!

        assertNull(outcome.ratings.firstOrNull { it.riotId == "달렸노#KR1" })
        assertNotNull(outcome.ratings.firstOrNull { it.riotId == "qkzxfh#KR1" })
        assertEquals("qkzxfh#KR1", outcome.histories.first { it.laneOpponent == "red0#KR1" }.riotId)
    }

    // ────────── 히스토리 ──────────

    @Test
    fun `맞대결이 없었던 경기는 NONE 으로 남는다`() {
        val aram = match(queueId = LaneScores.ARAM_QUEUE_ID)
        val outcome = RatingEngine.rate(aram, blueWinsEveryLane(aram), emptyMap(), ::identity)!!

        assertTrue(outcome.histories.all { it.laneResult == LaneResult.NONE })
        assertTrue(outcome.histories.all { it.laneOpponent == null })
        assertTrue(outcome.histories.all { it.laneDelta == 0.0 })
        // 팀 변동은 그대로 기록된다.
        assertTrue(outcome.histories.all { it.teamDelta != 0.0 })
    }

    @Test
    fun `승패를 판정할 수 없으면 아무것도 반영하지 않는다`() {
        val broken = match().participants.map { it.copy(win = false) }
        assertNull(RatingEngine.rate(match(participants = broken), blueWinsEveryLane(match()), emptyMap(), ::identity))
    }
}
