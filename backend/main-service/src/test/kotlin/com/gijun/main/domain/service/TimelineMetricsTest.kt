package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimelineMetricsTest {

    // ────────── 픽스처 ──────────

    private val positions = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

    /** 1~5 블루(승), 6~10 레드(패). 같은 인덱스끼리 라인 상대다 — 1 vs 6, 2 vs 7 … */
    private fun roster(participantIds: Boolean = true) =
        positions.mapIndexed { i, pos -> player("blue$i#KR1", if (participantIds) i + 1 else 0, 100, pos, win = true) } +
            positions.mapIndexed { i, pos -> player("red$i#KR1", if (participantIds) i + 6 else 0, 200, pos, win = false) }

    private fun player(riotId: String, pid: Int, teamId: Int, pos: Position, win: Boolean) = MatchParticipant(
        participantId = pid, riotId = riotId, champion = "Aatrox", championId = 266,
        team = if (teamId == 100) "blue" else "red", teamId = teamId, win = win,
        assignedPosition = pos.name,
    )

    private fun match(participants: List<MatchParticipant> = roster()) = Match(
        matchId = "KR_1", queueId = 3130, gameCreation = 1_700_000_000_000, gameDuration = 1_000,
        participants = participants.toMutableList(),
    )

    /**
     * 0~16분 매 분 프레임. 실측처럼 타임스탬프를 분당 22ms 씩 밀어 둔다.
     * 1번은 분당 골드 +100 · CS +1 씩 6번보다 앞서 간다. 나머지는 전원 같다.
     */
    private fun raw(events: String): String {
        val frames = (0..16).joinToString(",") { m ->
            val pf = (1..10).joinToString(",") { pid ->
                val lead = if (pid == 1) m else 0
                """"$pid":{"participantId":$pid,"totalGold":${500 + m * 300 + lead * 100},"xp":${m * 400},""" +
                    """"level":1,"minionsKilled":${m * 6 + lead},"jungleMinionsKilled":0}"""
            }
            val ev = if (m == 16) events else ""
            """{"timestamp":${m * 60_022L},"participantFrames":{$pf},"events":[$ev]}"""
        }
        return """{"frames":[$frames]}"""
    }

    private fun kill(ts: Long, killer: Int, victim: Int, assists: List<Int> = emptyList()) =
        """{"type":"CHAMPION_KILL","timestamp":$ts,"killerId":$killer,"victimId":$victim,"assistingParticipantIds":$assists}"""

    private fun metrics(events: List<String>, participants: List<MatchParticipant> = roster()) =
        TimelineMetrics.of(match(participants), TimelineParser.parse(raw(events.joinToString(","))))

    private fun TimelineMetrics.MatchMetrics.of(pid: Int) = players.single { it.participantId == pid }

    // ────────── 테스트 ──────────

    @Test
    fun `15분 라인 격차는 같은 포지션 상대와의 차이다`() {
        val m = metrics(emptyList())
        assertNotNull(m)
        m!!

        val top = m.of(1)
        assertEquals("red0#KR1", top.opponentRiotId)
        assertEquals(1_500, top.goldDiff15)
        assertEquals(15, top.csDiff15)
        assertEquals(0, top.xpDiff15)
        assertEquals(-1_500, m.of(6).goldDiff15)
        assertEquals(0, m.of(2).goldDiff15)
    }

    @Test
    fun `분 단위 곡선은 0분부터 마지막 프레임까지 빠짐없이 이어진다`() {
        val top = metrics(emptyList())!!.of(1)
        assertEquals((0..16).map { it * 100 }, top.goldDiffByMinute)
    }

    @Test
    fun `15분 전 킬 데스와 솔로킬 퍼블을 이벤트에서 센다`() {
        val m = metrics(listOf(
            kill(ts = 120_000, killer = 1, victim = 6),                      // 퍼블, 솔로킬
            kill(ts = 700_000, killer = 7, victim = 1, assists = listOf(8)), // 1번 첫 데스
            kill(ts = 1_000_000, killer = 1, victim = 6, assists = listOf(2)), // 15분 이후
        ))!!

        val top = m.of(1)
        assertEquals(1, top.earlyKills)
        assertEquals(1, top.earlyDeaths)
        assertEquals(1, top.soloKills, "어시스트가 붙은 킬은 솔로킬이 아니다")
        assertTrue(top.firstBlood)
        assertEquals(700_000, top.firstDeathMs)

        assertEquals(1, m.of(8).earlyAssists)
        assertEquals(0, m.of(2).earlyAssists, "15분 이후 어시스트는 초반 지표에 안 들어간다")
        assertFalse(m.of(2).firstBlood)
        assertNull(m.of(3).firstDeathMs)
    }

    @Test
    fun `15분 골드가 앞선 팀을 잡는다`() {
        val m = metrics(emptyList())!!
        assertEquals(100, m.goldLeadTeamAt15)
        assertEquals(1_500, m.teamGoldGapAt15)
        assertEquals(100, m.winnerTeamId)
    }

    @Test
    fun `participantId 가 없는 옛 경기는 계산하지 않는다`() {
        assertNull(metrics(emptyList(), roster(participantIds = false)))
    }

    @Test
    fun `포지션이 깨져 상대가 없으면 격차는 비워 두되 킬 지표는 낸다`() {
        val broken = roster().map { if (it.participantId == 6) it.copy(assignedPosition = Position.MID.name) else it }
        val m = metrics(listOf(kill(ts = 60_000, killer = 1, victim = 7)), broken)!!

        val top = m.of(1)
        assertNull(top.opponentRiotId)
        assertNull(top.goldDiff15)
        assertTrue(top.goldDiffByMinute.isEmpty())
        assertEquals(1, top.earlyKills)
    }
}
