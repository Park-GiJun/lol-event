package com.gijun.main.application.handler.query

import com.gijun.main.application.port.out.MatchPeriodSummary
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.PositionCount
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.service.SessionClock
import com.gijun.main.domain.session.exception.InvalidSessionDateException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GetSessionDetailHandlerTest {
    private val positions = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

    // ────────── 픽스처 ──────────

    private fun kst(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, SessionClock.ZONE).toInstant().toEpochMilli()

    /** [blueWins] 로 승패를 뒤집는다. 같은 열 명이 매 판 같은 자리에 선다. */
    private fun roster(blueWins: Boolean): List<MatchParticipant> =
        (1..10).map { pid ->
            val blue = pid <= 5
            MatchParticipant(
                participantId = pid,
                riotId = "p$pid#KR1",
                champion = "Champ$pid",
                championId = pid,
                team = if (blue) "blue" else "red",
                teamId = if (blue) 100 else 200,
                win = blue == blueWins,
                kills = if (blue == blueWins) 4 else 1,
                deaths = if (blue == blueWins) 1 else 4,
                assists = 2,
                assignedPosition = positions[(pid - 1) % 5].name,
            )
        }

    private fun match(
        matchId: String,
        at: Long,
        blueWins: Boolean = true,
        duration: Int = 1_800,
    ) = Match(
        matchId = matchId,
        queueId = 3130,
        gameCreation = at,
        gameDuration = duration,
        participants = roster(blueWins).toMutableList(),
    )

    /** 0~16분 프레임. [blueLeadPerMinute] 만큼 블루 1번이 앞서 간다(음수면 뒤진다). */
    private fun raw(blueLeadPerMinute: Int = 100): String {
        val frames =
            (0..16).joinToString(",") { m ->
                val pf =
                    (1..10).joinToString(",") { pid ->
                        val lead = if (pid == 1) m * blueLeadPerMinute else 0
                        """"$pid":{"participantId":$pid,"totalGold":${5_000 + m * 300 + lead},"xp":${m * 400},""" +
                            """"minionsKilled":${m * 6},"jungleMinionsKilled":0,"position":{"x":1512,"y":6699}}"""
                    }
                """{"timestamp":${m * 60_000L},"participantFrames":{$pf},"events":[]}"""
            }
        return """{"frames":[$frames]}"""
    }

    private fun handler(
        matches: List<Match>,
        raws: Map<String, String> = emptyMap(),
    ): GetSessionDetailHandler {
        val port =
            object : MatchPersistencePort {
                override fun save(match: Match) = match

                override fun existsByMatchId(matchId: String) = true

                override fun findByMatchId(matchId: String): Match? = matches.firstOrNull { it.matchId == matchId }

                override fun findAllWithParticipants(queueIds: List<Int>) = matches

                override fun findPageWithParticipants(
                    queueIds: List<Int>,
                    page: Int,
                    size: Int,
                ) = matches

                override fun findPeriodSummary(queueIds: List<Int>) = MatchPeriodSummary(null, null, 0, 0)

                override fun deleteByMatchId(matchId: String) {}

                override fun countByQueueIds(queueIds: List<Int>) = matches.size.toLong()

                override fun findAllOrderedByGameCreation() = matches

                override fun findInPeriodWithParticipants(
                    queueIds: List<Int>,
                    fromMs: Long,
                    untilMs: Long,
                ) = matches.filter { it.gameCreation in fromMs until untilMs }.sortedBy { it.gameCreation }

                override fun updateAssignedPositions(updates: Map<Long, String>) {}

                override fun saveTimelineRaw(
                    matchId: String,
                    raw: String,
                ) {}

                override fun findTimelineRaw(matchIds: Collection<String>) = raws.filterKeys { it in matchIds }

                override fun updateLaneMethods(updates: Map<String, LaneMethod>) {}

                override fun findPositionCounts() = emptyList<PositionCount>()
            }
        val cache =
            object : StatsCachePort {
                override fun <T> getOrCompute(
                    key: String,
                    compute: () -> T,
                ): T = compute()

                override fun evictAll() {}

                override fun evictByPrefix(prefix: String) {}
            }
        return GetSessionDetailHandler(port, cache)
    }

    // ────────── 경계 ──────────

    @Test
    fun `새벽 경기는 전날 세션에 들어간다`() {
        val matches =
            listOf(
                match("KR_1", kst(2026, 9, 14, 22, 0)),
                match("KR_2", kst(2026, 9, 15, 1, 30)),
                match("KR_3", kst(2026, 9, 15, 20, 0)),
            )
        val result = handler(matches).getSessionDetail("2026-09-14", "normal")!!

        assertEquals(2, result.games, "22시 판과 다음날 1시 30분 판이 같은 세션이다")
        assertEquals(listOf("KR_1", "KR_2"), result.matches.map { it.matchId })
        assertEquals(kst(2026, 9, 14, 22, 0), result.firstGameAt)
        assertEquals(kst(2026, 9, 15, 1, 30), result.lastGameAt)
    }

    @Test
    fun `경기가 없는 날짜는 null 이다`() {
        val matches = listOf(match("KR_1", kst(2026, 9, 14, 22, 0)))

        assertNull(handler(matches).getSessionDetail("2026-09-20", "normal"), "이때 404 로 간다")
    }

    @Test
    fun `날짜 형식이 틀리면 검증 예외다`() {
        val handler = handler(emptyList())

        assertThrows(InvalidSessionDateException::class.java) { handler.getSessionDetail("2026-9-14", "normal") }
        assertThrows(InvalidSessionDateException::class.java) { handler.getSessionDetail("어제", "normal") }
    }

    // ────────── 집계 ──────────

    @Test
    fun `팀 승수와 총 킬을 센다`() {
        val matches =
            listOf(
                match("KR_1", kst(2026, 9, 14, 21, 0), blueWins = true),
                match("KR_2", kst(2026, 9, 14, 22, 0), blueWins = false),
                match("KR_3", kst(2026, 9, 14, 23, 0), blueWins = true),
            )
        val result = handler(matches).getSessionDetail("2026-09-14", "normal")!!

        assertEquals(3, result.games)
        assertEquals(2, result.team100Wins)
        assertEquals(1, result.team200Wins)
        assertEquals(75, result.totalKills, "판마다 이긴 쪽 5명이 4킬, 진 쪽 5명이 1킬 = 25킬")
        assertEquals(90, result.totalDurationMin)
    }

    @Test
    fun `사람별 집계는 승수와 KDA 순으로 나온다`() {
        val matches =
            listOf(
                match("KR_1", kst(2026, 9, 14, 21, 0), blueWins = true),
                match("KR_2", kst(2026, 9, 14, 22, 0), blueWins = true),
            )
        val result = handler(matches).getSessionDetail("2026-09-14", "normal")!!

        assertEquals(10, result.players.size)
        val best = result.players.first()
        assertTrue(best.riotId in listOf("p1#KR1", "p2#KR1", "p3#KR1", "p4#KR1", "p5#KR1"), "두 판 다 이긴 블루")
        assertEquals(2, best.games)
        assertEquals(2, best.wins)
        assertEquals(8, best.kills)
        assertEquals(6.0, best.kda, "(8킬 + 4어시) / 2데스")
    }

    @Test
    fun `최대 연승은 사람 기준으로 센다`() {
        // 팀이 매 판 바뀌는 내전이라 팀 기준으로 세면 계속 끊긴다.
        val matches =
            listOf(
                match("KR_1", kst(2026, 9, 14, 21, 0), blueWins = true),
                match("KR_2", kst(2026, 9, 14, 22, 0), blueWins = true),
                match("KR_3", kst(2026, 9, 14, 23, 0), blueWins = false),
            )
        val result = handler(matches).getSessionDetail("2026-09-14", "normal")!!

        // 블루 다섯 명은 이기고 이기고 졌다 → 2연승. 레드 다섯 명은 지고 지고 이겼다 → 2연패.
        assertEquals(2, result.longestWinStreak!!.length)
        assertTrue(result.longestWinStreak!!.riotId in (1..5).map { "p$it#KR1" })
        assertEquals(2, result.longestLossStreak!!.length)
        assertTrue(result.longestLossStreak!!.riotId in (6..10).map { "p$it#KR1" })
    }

    @Test
    fun `한 판만 이긴 건 연승으로 보지 않는다`() {
        val matches =
            listOf(
                match("KR_1", kst(2026, 9, 14, 21, 0), blueWins = true),
                match("KR_2", kst(2026, 9, 14, 22, 0), blueWins = false),
            )
        val result = handler(matches).getSessionDetail("2026-09-14", "normal")!!

        assertNull(result.longestWinStreak, "번갈아 이기면 아무도 2연승이 아니다")
        assertNull(result.longestLossStreak)
    }

    @Test
    fun `가장 길고 짧은 판을 집는다`() {
        val matches =
            listOf(
                match("KR_1", kst(2026, 9, 14, 21, 0), duration = 1_200),
                match("KR_2", kst(2026, 9, 14, 22, 0), duration = 2_400),
            )
        val result = handler(matches).getSessionDetail("2026-09-14", "normal")!!

        assertEquals("KR_2", result.longestGame!!.matchId)
        assertEquals("KR_1", result.shortestGame!!.matchId)
    }

    // ────────── 타임라인 ──────────

    @Test
    fun `타임라인이 없는 경기도 목록에 남고 분모에서만 빠진다`() {
        val matches =
            listOf(
                match("KR_1", kst(2026, 9, 14, 21, 0)),
                match("KR_2", kst(2026, 9, 14, 22, 0)),
            )
        val result = handler(matches, raws = mapOf("KR_1" to raw())).getSessionDetail("2026-09-14", "normal")!!

        assertEquals(2, result.games)
        assertEquals(1, result.timelineGames, "타임라인 수치의 분모는 전체 경기가 아니다")
        assertTrue(result.matches.first { it.matchId == "KR_1" }.hasTimeline)
        assertFalse(result.matches.first { it.matchId == "KR_2" }.hasTimeline)
        assertTrue(
            result.matches
                .first { it.matchId == "KR_2" }
                .teamGoldDiffByMinute
                .isEmpty(),
        )
    }

    @Test
    fun `스파크라인용 골드 격차 곡선을 경기마다 담는다`() {
        val matches = listOf(match("KR_1", kst(2026, 9, 14, 21, 0)))
        val result = handler(matches, raws = mapOf("KR_1" to raw())).getSessionDetail("2026-09-14", "normal")!!

        val curve = result.matches.single().teamGoldDiffByMinute
        assertEquals(17, curve.size, "0~16분")
        assertEquals(0, curve.first())
        assertEquals(1_600, curve.last(), "블루가 분당 100씩 앞서 16분이면 1,600")
    }

    @Test
    fun `15분에 뒤지고도 이긴 경기를 역전으로 집는다`() {
        val matches =
            listOf(
                // 블루가 앞서다 이긴 판 — 역전이 아니다.
                match("KR_1", kst(2026, 9, 14, 21, 0), blueWins = true),
                // 블루가 뒤지다 이긴 판 — 역전이다.
                match("KR_2", kst(2026, 9, 14, 22, 0), blueWins = true),
            )
        val raws = mapOf("KR_1" to raw(blueLeadPerMinute = 100), "KR_2" to raw(blueLeadPerMinute = -200))
        val result = handler(matches, raws).getSessionDetail("2026-09-14", "normal")!!

        assertEquals("KR_2", result.biggestComeback!!.matchId)
        assertTrue(result.biggestComeback!!.goldDiffAt15!! < 0, "이긴 팀이 15분에 뒤지고 있었다")
    }

    @Test
    fun `역전이 없으면 비워 둔다`() {
        val matches = listOf(match("KR_1", kst(2026, 9, 14, 21, 0), blueWins = true))
        val result = handler(matches, mapOf("KR_1" to raw())).getSessionDetail("2026-09-14", "normal")!!

        assertNull(result.biggestComeback)
    }

    @Test
    fun `한타가 없으면 영이다`() {
        val matches = listOf(match("KR_1", kst(2026, 9, 14, 21, 0)))
        val result = handler(matches, mapOf("KR_1" to raw())).getSessionDetail("2026-09-14", "normal")!!

        assertEquals(0, result.teamFights, "픽스처에 킬 이벤트가 없다")
        assertEquals(0, result.team100FightWins)
    }
}
