package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LaneScoresTest {

    // ────────── 픽스처 ──────────

    private fun player(
        riotId: String,
        participantId: Int,
        teamId: Int,
        position: Position,
        gold: Int = 12_000,
        damageSelfMitigated: Int = 0,
        win: Boolean = true,
    ) = MatchParticipant(
        participantId = participantId,
        riotId = riotId,
        champion = "Aatrox",
        team = if (teamId == 100) "blue" else "red",
        teamId = teamId,
        win = win,
        gold = gold,
        damageSelfMitigated = damageSelfMitigated,
        assignedPosition = position.name,
    )

    /** 10명이 포지션을 하나씩 채운 정상 경기. */
    private fun match(
        queueId: Int = 3130,
        gameDuration: Int = 1_800,
        participants: List<MatchParticipant> = roster(),
        timelineRaw: String? = null,
    ) = Match(
        matchId = "KR_1",
        queueId = queueId,
        gameCreation = 1_700_000_000_000,
        gameDuration = gameDuration,
        timelineRaw = timelineRaw,
        participants = participants.toMutableList(),
    )

    private fun roster(): List<MatchParticipant> {
        val positions = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)
        return positions.mapIndexed { i, pos -> player("blue$i#KR1", i + 1, 100, pos) } +
            positions.mapIndexed { i, pos -> player("red$i#KR1", i + 6, 200, pos, win = false) }
    }

    /**
     * 실제 LCU 응답과 같은 모양의 타임라인. 프레임 타임스탬프는 일부러 조금씩 밀어 둔다 —
     * 실측하면 15분 프레임이 900,330ms 처럼 항상 15분을 살짝 넘긴다.
     */
    private fun timeline(frames: List<Pair<Long, Map<Int, Pair<Int, Int>>>>): String {
        val body = frames.joinToString(",") { (ts, players) ->
            val pf = players.entries.joinToString(",") { (pid, gx) ->
                """"$pid":{"participantId":$pid,"totalGold":${gx.first},"xp":${gx.second}}"""
            }
            """{"timestamp":$ts,"participantFrames":{$pf},"events":[]}"""
        }
        return """{"frames":[$body]}"""
    }

    /** 1~10번에게 같은 값을 주되, [winners] 에 든 번호만 골드를 크게 준다. */
    private fun frameAt(ts: Long, winners: Set<Int>): Pair<Long, Map<Int, Pair<Int, Int>>> =
        ts to (1..10).associateWith { pid ->
            if (pid in winners) 6_000 to 5_000 else 4_000 to 4_000
        }

    // ────────── T1: TIMELINE_15 ──────────

    @Test
    fun `타임라인이 있으면 15분 프레임으로 판정한다`() {
        // 14분 프레임에서는 1번이 앞서고, 15분 프레임에서는 6번이 앞선다.
        // 15분 프레임을 골랐다면 6번이 이겨야 한다.
        val raw = timeline(
            listOf(
                frameAt(840_297, setOf(1)),
                frameAt(900_330, setOf(6)),
                frameAt(960_337, setOf(1)),
            )
        )
        val scored = LaneScores.of(match(), TimelineParser.parse(raw))

        assertEquals(LaneMethod.TIMELINE_15, scored.method)
        assertTrue(scored.scores.getValue("red0#KR1") > scored.scores.getValue("blue0#KR1"))
    }

    @Test
    fun `프레임 드리프트로 15분을 살짝 넘겨도 15분 프레임을 잡는다`() {
        // 명세를 문자 그대로 읽어 timestamp <= 900,000 만 허용하면 14분 프레임이 잡힌다.
        // 드리프트는 누적되고 부호가 항상 양수라, 그러면 매 경기가 한 칸씩 밀린다.
        val raw = timeline(listOf(frameAt(840_297, setOf(1)), frameAt(900_330, setOf(6))))
        val scored = LaneScores.of(match(), TimelineParser.parse(raw))

        assertTrue(scored.scores.getValue("red0#KR1") > scored.scores.getValue("blue0#KR1"))
    }

    @Test
    fun `15분 전에 끝난 경기는 마지막 프레임을 쓴다`() {
        val raw = timeline(listOf(frameAt(0, emptySet()), frameAt(600_226, setOf(6))))
        val scored = LaneScores.of(match(gameDuration = 700), TimelineParser.parse(raw))

        assertEquals(LaneMethod.TIMELINE_15, scored.method)
        assertTrue(scored.scores.getValue("red0#KR1") > scored.scores.getValue("blue0#KR1"))
    }

    @Test
    fun `점수는 골드에 경험치를 그대로 더한 값이다`() {
        val raw = timeline(listOf(frameAt(900_330, setOf(1))))
        val scored = LaneScores.of(match(), TimelineParser.parse(raw))

        // 1번은 승자 값(6000 골드 + 5000 경험치), 나머지는 4000 + 4000.
        assertEquals(11_000.0, scored.scores.getValue("blue0#KR1"))
        assertEquals(8_000.0, scored.scores.getValue("blue1#KR1"))
    }

    @Test
    fun `participantId 가 없으면 타임라인을 쓰지 못하고 내려간다`() {
        // 타임라인 저장 이전에 수집된 경기다. 프레임과 사람을 이을 고리가 없다.
        val legacy = roster().map { it.copy(participantId = 0) }
        val raw = timeline(listOf(frameAt(900_330, setOf(1))))
        val scored = LaneScores.of(match(participants = legacy, timelineRaw = raw), TimelineParser.parse(raw))

        assertEquals(LaneMethod.LEGACY_FINAL, scored.method)
    }

    // ────────── T3: LEGACY_FINAL ──────────

    @Test
    fun `타임라인이 없으면 경기 종료 시점 누적값으로 판정한다`() {
        val scored = LaneScores.of(match(), null)
        assertEquals(LaneMethod.LEGACY_FINAL, scored.method)
    }

    @Test
    fun `피해감소 보정은 탱커의 골드 열세를 메운다`() {
        // 탑 라인: 블루가 골드는 500 적지만 피해를 10만 더 받아냈다.
        // 보정 계수 0.007 이면 700 > 500 이라 뒤집힌다.
        val tanky = roster().map {
            when (it.riotId) {
                "blue0#KR1" -> it.copy(gold = 11_500, damageSelfMitigated = 100_000)
                "red0#KR1" -> it.copy(gold = 12_000, damageSelfMitigated = 0)
                else -> it
            }
        }
        val scored = LaneScores.of(match(participants = tanky), null)

        assertEquals(LaneMethod.LEGACY_FINAL, scored.method)
        assertTrue(scored.scores.getValue("blue0#KR1") > scored.scores.getValue("red0#KR1"))
    }

    @Test
    fun `피해감소 보정은 15분 기준에는 적용하지 않는다`() {
        // 같은 상황을 타임라인으로 판정하면 보정이 없어야 한다 — 15분 기준에는 탱커 왜곡이
        // 거의 없어서, 여기에 또 얹으면 이중 보정이 된다.
        val tanky = roster().map {
            when (it.riotId) {
                "blue0#KR1" -> it.copy(damageSelfMitigated = 100_000)
                else -> it
            }
        }
        val raw = timeline(listOf(frameAt(900_330, emptySet())))
        val scored = LaneScores.of(match(participants = tanky), TimelineParser.parse(raw))

        assertEquals(LaneMethod.TIMELINE_15, scored.method)
        assertEquals(scored.scores.getValue("blue0#KR1"), scored.scores.getValue("red0#KR1"))
    }

    // ────────── 맞대결 구성 ──────────

    @Test
    fun `정상 경기는 다섯 쌍이 나온다`() {
        assertEquals(5, LaneScores.duels(match()).size)
    }

    @Test
    fun `한 팀에 같은 포지션이 둘이면 그 자리는 통째로 버린다`() {
        val broken = roster().map {
            if (it.riotId == "blue1#KR1") it.copy(assignedPosition = Position.TOP.name) else it
        }
        val duels = LaneScores.duels(match(participants = broken))

        // TOP 에 세 명(블루 둘, 레드 하나)이라 쌍이 안 되고, JUNGLE 은 레드만 남아 역시 안 된다.
        assertEquals(3, duels.size)
        assertNull(duels.firstOrNull { it.first.assignedPosition == Position.TOP.name })
    }

    @Test
    fun `칼바람은 맞대결을 만들지 않는다`() {
        // 매치 저장 시 포지션 추정기가 큐를 가리지 않고 돌기 때문에 칼바람 참가자에게도
        // 포지션이 붙어 있다. 그건 데이터가 아니라 추정기의 창작물이다.
        assertTrue(LaneScores.duels(match(queueId = LaneScores.ARAM_QUEUE_ID)).isEmpty())
    }

    @Test
    fun `쌍은 항상 서로 다른 팀에서 하나씩 나온다`() {
        LaneScores.duels(match()).forEach { (a, b) ->
            assertNotNull(a)
            assertTrue(a.teamId != b.teamId, "같은 팀끼리 맞대결이 잡혔다: ${a.riotId} vs ${b.riotId}")
        }
    }
}
