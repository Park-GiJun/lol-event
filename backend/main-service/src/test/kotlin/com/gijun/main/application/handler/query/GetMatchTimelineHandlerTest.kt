package com.gijun.main.application.handler.query

import com.gijun.main.application.port.out.MatchPeriodSummary
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.PositionCount
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 경기 한 판을 화면이 쓰는 모양으로 옮기는 부분.
 *
 * 좌표 판정은 MapGeometryTest, 교전 묶기는 TeamFightDetectorTest, 라인 격차는 TimelineMetricsTest 가
 * 각자 고정한다. 여기서는 **뒤집기와 결손 처리**를 본다 — 건물 팀을 뒤집는지, 타임라인이 없는
 * 경기가 404 가 아닌지.
 */
class GetMatchTimelineHandlerTest {

    private val positions = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

    // ────────── 픽스처 ──────────

    /** 1~5 블루(승), 6~10 레드(패). 같은 인덱스끼리 라인 상대다. */
    private fun roster(participantIds: Boolean = true): List<MatchParticipant> = (1..10).map { pid ->
        val blue = pid <= 5
        MatchParticipant(
            participantId = if (participantIds) pid else 0,
            riotId = "p$pid#KR1",
            champion = "Champ$pid",
            championId = pid,
            team = if (blue) "blue" else "red",
            teamId = if (blue) 100 else 200,
            win = blue,
            assignedPosition = positions[(pid - 1) % 5].name,
        )
    }

    /** 블루 탑 내부 포탑 자리. MapGeometryTest 와 같은 실측 좌표다. */
    private val blueTopLane = """"position":{"x":1512,"y":6699}"""

    /** 0~16분 프레임. 1번(블루 탑)만 분당 골드 +100 씩 앞서 간다. */
    private fun raw(events: String = "", lastMinute: Int = 16, skipMinute: Int? = null): String {
        val frames = (0..lastMinute).filter { it != skipMinute }.joinToString(",") { m ->
            val pf = (1..10).joinToString(",") { pid ->
                val lead = if (pid == 1) m * 100 else 0
                """"$pid":{"participantId":$pid,"totalGold":${500 + m * 300 + lead},"xp":${m * 400},""" +
                    """"currentGold":${m * 10},"level":${1 + m / 3},"minionsKilled":${m * 6},""" +
                    """"jungleMinionsKilled":0,$blueTopLane}"""
            }
            val ev = if (m == lastMinute) events else ""
            """{"timestamp":${m * 60_000L},"participantFrames":{$pf},"events":[$ev]}"""
        }
        return """{"frames":[$frames]}"""
    }

    /** 이벤트는 실제 원본처럼 union 의 다른 필드까지 채워 보낸다. */
    private fun tower(ts: Long, destroyedTeam: Int, killer: Int) =
        """{"type":"BUILDING_KILL","timestamp":$ts,"teamId":$destroyedTeam,"killerId":$killer,""" +
            """"buildingType":"TOWER_BUILDING","towerType":"OUTER_TURRET","laneType":"TOP_LANE",""" +
            """"assistingParticipantIds":[2],"position":{"x":981,"y":10441}}"""

    private fun dragon(ts: Long, killer: Int) =
        """{"type":"ELITE_MONSTER_KILL","timestamp":$ts,"killerId":$killer,""" +
            """"monsterType":"DRAGON","monsterSubType":"FIRE_DRAGON","assistingParticipantIds":[3],""" +
            """"position":{"x":9910,"y":4530}}"""

    private fun kill(ts: Long, killer: Int, victim: Int) =
        """{"type":"CHAMPION_KILL","timestamp":$ts,"killerId":$killer,"victimId":$victim,""" +
            """"assistingParticipantIds":[],"position":{"x":7300,"y":7300}}"""

    private fun handler(
        raw: String? = raw(),
        participants: List<MatchParticipant> = roster(),
        matchExists: Boolean = true,
    ): GetMatchTimelineHandler {
        val match = Match(
            matchId = "KR_1", queueId = 3130, gameCreation = 1, gameDuration = 1_020,
            participants = participants.toMutableList(),
        )
        val port = object : MatchPersistencePort {
            override fun save(match: Match) = match
            override fun existsByMatchId(matchId: String) = matchExists
            override fun findByMatchId(matchId: String): Match? = if (matchExists) match else null
            override fun findAllWithParticipants(queueIds: List<Int>) = listOf(match)
            override fun findPageWithParticipants(queueIds: List<Int>, page: Int, size: Int) = listOf(match)
            override fun findPeriodSummary(queueIds: List<Int>) = MatchPeriodSummary(null, null, 0, 0)
            override fun deleteByMatchId(matchId: String) {}
            override fun countByQueueIds(queueIds: List<Int>) = 1L
            override fun findAllOrderedByGameCreation() = listOf(match)
            override fun updateAssignedPositions(updates: Map<Long, String>) {}
            override fun saveTimelineRaw(matchId: String, raw: String) {}
            override fun findTimelineRaw(matchIds: Collection<String>) =
                raw?.let { mapOf("KR_1" to it) } ?: emptyMap()
            override fun updateLaneMethods(updates: Map<String, LaneMethod>) {}
            override fun findPositionCounts() = emptyList<PositionCount>()
        }
        val cache = object : StatsCachePort {
            override fun <T> getOrCompute(key: String, compute: () -> T): T = compute()
            override fun evictAll() {}
            override fun evictByPrefix(prefix: String) {}
        }
        return GetMatchTimelineHandler(port, cache)
    }

    // ────────── 결손 ──────────

    @Test
    fun `경기가 없으면 null 이다`() {
        assertNull(handler(matchExists = false).getMatchTimeline("KR_1"), "이때만 404 로 간다")
    }

    @Test
    fun `타임라인이 없는 경기는 빈 결과를 돌려준다`() {
        val result = handler(raw = null).getMatchTimeline("KR_1")

        assertNotNull(result)
        assertFalse(result!!.hasTimeline, "404 를 주면 대부분의 경기 상세 화면이 에러로 뜬다")
        assertEquals(1_020_000L, result.durationMs, "경기 길이는 타임라인과 무관하게 채운다")
        assertTrue(result.participants.isEmpty())
        assertTrue(result.kills.isEmpty())
        assertTrue(result.teamFights.isEmpty())
    }

    @Test
    fun `participantId 가 없는 옛 경기도 빈 결과다`() {
        // 프레임과 사람을 이을 고리가 없으면 타임라인이 있어도 쓸 수 없다.
        val result = handler(participants = roster(participantIds = false)).getMatchTimeline("KR_1")!!

        assertFalse(result.hasTimeline)
    }

    @Test
    fun `중간 프레임이 빠지면 거기서 자른다`() {
        val result = handler(raw = raw(skipMinute = 8)).getMatchTimeline("KR_1")!!

        assertEquals(7, result.lastMinute, "8분이 없으면 7분까지만 쓴다")
        assertEquals(8, result.teamGoldDiffByMinute.size)
    }

    // ────────── 곡선 ──────────

    @Test
    fun `팀 골드 격차는 블루 빼기 레드다`() {
        val result = handler().getMatchTimeline("KR_1")!!

        assertEquals(16, result.lastMinute)
        assertEquals(0, result.teamGoldDiffByMinute.first(), "0분에는 열 명이 같다")
        assertEquals(1_600, result.teamGoldDiffByMinute.last(), "1번이 분당 100씩 앞서 16분이면 1,600")
    }

    @Test
    fun `참가자 시계열은 분 단위로 index 가 맞는다`() {
        val top = handler().getMatchTimeline("KR_1")!!.participants.single { it.participantId == 1 }

        assertEquals(17, top.goldByMinute.size, "0~16분이면 17개")
        assertEquals(500, top.goldByMinute.first())
        assertEquals(160, top.currentGoldByMinute.last(), "손에 든 골드도 같이 담는다")
        assertEquals(6, top.levelByMinute.last())
        assertEquals(17, top.positionsByMinute.size)
        assertEquals(1_512, top.positionsByMinute.last()!!.x)
    }

    @Test
    fun `팀 합계는 그 팀 다섯 명만 더한다`() {
        val blue = handler().getMatchTimeline("KR_1")!!.teams.single { it.teamId == 100 }
        val red = handler().getMatchTimeline("KR_1")!!.teams.single { it.teamId == 200 }

        assertTrue(blue.win)
        assertFalse(red.win)
        assertEquals(2_500, blue.goldByMinute.first(), "0분에 500씩 다섯 명")
        assertEquals(2_500, red.goldByMinute.first())
    }

    // ────────── 뒤집기 ──────────

    @Test
    fun `부서진 건물의 팀을 먹은 팀으로 뒤집는다`() {
        // 원본의 teamId 는 파괴당한 쪽이다. 그대로 내보내면 화면 색이 반대로 칠해진다.
        val result = handler(raw = raw(events = tower(ts = 900_000, destroyedTeam = 200, killer = 1)))
            .getMatchTimeline("KR_1")!!

        val objective = result.objectives.single()
        assertEquals(100, objective.killingTeamId, "레드 포탑이 부서졌으니 블루가 먹었다")
        assertEquals("TOWER_BUILDING", objective.kind)
        assertEquals("OUTER_TURRET", objective.towerType)
        assertEquals("TOP_LANE", objective.lane)
        assertEquals(listOf(2), objective.assistParticipantIds)
    }

    @Test
    fun `오브젝트는 막타를 친 사람의 팀이 먹은 팀이다`() {
        val result = handler(raw = raw(events = dragon(ts = 900_000, killer = 7))).getMatchTimeline("KR_1")!!

        val objective = result.objectives.single()
        assertEquals(200, objective.killingTeamId, "건물과 달리 오브젝트는 뒤집지 않는다")
        assertEquals("DRAGON", objective.kind)
        assertEquals("FIRE_DRAGON", objective.subType)
    }

    @Test
    fun `킬은 희생자의 팀을 뒤집어 먹은 팀을 정한다`() {
        val result = handler(raw = raw(events = kill(ts = 900_000, killer = 0, victim = 6)))
            .getMatchTimeline("KR_1")!!

        val kill = result.kills.single()
        assertEquals(100, kill.killingTeamId, "막타가 포탑이어도 레드가 죽었으면 블루의 킬이다")
        assertEquals(0, kill.killerParticipantId)
        assertEquals(15, kill.minute)
        assertEquals("MID_LANE", kill.region, "맵 중앙은 강이 아니라 미드다")
    }

    @Test
    fun `킬과 오브젝트는 서로의 목록에 섞이지 않는다`() {
        val events = listOf(
            kill(ts = 900_000, killer = 1, victim = 6),
            tower(ts = 905_000, destroyedTeam = 200, killer = 1),
            dragon(ts = 910_000, killer = 2),
        ).joinToString(",")
        val result = handler(raw = raw(events = events)).getMatchTimeline("KR_1")!!

        assertEquals(1, result.kills.size)
        assertEquals(2, result.objectives.size)
    }

    // ────────── 교전 ──────────

    @Test
    fun `교전 구간과 전리품을 함께 낸다`() {
        val events = listOf(
            kill(ts = 900_000, killer = 1, victim = 6),
            kill(ts = 904_000, killer = 2, victim = 7),
            kill(ts = 908_000, killer = 3, victim = 8),
            dragon(ts = 920_000, killer = 2),
        ).joinToString(",")
        val fight = handler(raw = raw(events = events)).getMatchTimeline("KR_1")!!.teamFights.single()

        assertTrue(fight.isTeamFight)
        assertEquals(3, fight.team100Kills)
        assertEquals(100, fight.winnerTeamId)
        assertEquals(15, fight.startMinute)
        assertEquals(listOf("DRAGON"), fight.objectiveKinds)
        assertEquals(listOf(1, 2, 3, 6, 7, 8), fight.participantIds)
    }

    // ────────── 좌표 지표 ──────────

    @Test
    fun `라인 점유는 자리마다 다르게 매겨진다`() {
        val result = handler().getMatchTimeline("KR_1")!!

        // 픽스처는 열 명 모두 블루 탑 라인에 세워 뒀다.
        val top = result.participants.single { it.participantId == 1 }
        assertEquals(100.0, top.laneShareRate)
        assertEquals(16, top.framesSampled, "0분 프레임은 분모에서 빠진다")

        val jungler = result.participants.single { it.participantId == 2 }
        assertNull(jungler.laneShareRate, "정글은 라인 지표를 매기지 않는다")

        val redTop = result.participants.single { it.participantId == 6 }
        assertEquals(100.0, redTop.enemyHalfRate, "레드가 블루 탑에 있으면 전부 상대 진영이다")
    }
}
