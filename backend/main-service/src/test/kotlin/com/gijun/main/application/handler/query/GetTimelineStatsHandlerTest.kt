package com.gijun.main.application.handler.query

import com.gijun.main.application.port.out.MatchPeriodSummary
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.PositionCount
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.service.RiotIdNormalizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 타임라인 지표를 선수·포지션·챔피언 단위로 묶는 부분.
 * 경기 단위 계산 자체는 TimelineMetricsTest 가 고정한다.
 */
class GetTimelineStatsHandlerTest {

    private val positions = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

    /** 1~5 블루(승), 6~10 레드(패). 챔피언은 번호마다 다르고, 1번(블루 탑)만 Aatrox 다. */
    private fun roster(): List<MatchParticipant> = (1..10).map { pid ->
        val blue = pid <= 5
        MatchParticipant(
            participantId = pid,
            riotId = "p$pid#KR1",
            champion = if (pid == 1) "Aatrox" else "Champ$pid",
            championId = pid,
            team = if (blue) "blue" else "red",
            teamId = if (blue) 100 else 200,
            win = blue,
            assignedPosition = positions[(pid - 1) % 5].name,
        )
    }

    /** 0~16분 매 분 프레임. 1번(블루 탑)만 분당 골드 +100 씩 앞서 간다. */
    private fun raw(): String {
        val frames = (0..16).joinToString(",") { m ->
            val pf = (1..10).joinToString(",") { pid ->
                val lead = if (pid == 1) m * 100 else 0
                """"$pid":{"participantId":$pid,"totalGold":${500 + m * 300 + lead},"xp":${m * 400},"minionsKilled":${m * 6},"jungleMinionsKilled":0}"""
            }
            """{"timestamp":${m * 60_000L},"participantFrames":{$pf},"events":[]}"""
        }
        return """{"frames":[$frames]}"""
    }

    private fun handler(aliases: Map<String, String> = emptyMap()): GetTimelineStatsHandler {
        val match = Match(
            matchId = "KR_1", queueId = 3130, gameCreation = 1, gameDuration = 1_000,
            participants = roster().toMutableList(),
        )
        val port = object : MatchPersistencePort {
            override fun save(match: Match) = match
            override fun existsByMatchId(matchId: String) = false
            override fun findByMatchId(matchId: String): Match? = null
            override fun findAllWithParticipants(queueIds: List<Int>) = listOf(match)
            override fun findPageWithParticipants(queueIds: List<Int>, page: Int, size: Int) = listOf(match)
            override fun findPeriodSummary(queueIds: List<Int>) = MatchPeriodSummary(null, null, 0, 0)
            override fun deleteByMatchId(matchId: String) {}
            override fun countByQueueIds(queueIds: List<Int>) = 1L
            override fun findAllOrderedByGameCreation() = listOf(match)
            override fun updateAssignedPositions(updates: Map<Long, String>) {}
            override fun saveTimelineRaw(matchId: String, raw: String) {}
            override fun findTimelineRaw(matchIds: Collection<String>) = mapOf("KR_1" to raw())
            override fun updateLaneMethods(updates: Map<String, LaneMethod>) {}
            override fun findPositionCounts() = emptyList<PositionCount>()
        }
        val cache = object : StatsCachePort {
            override fun <T> getOrCompute(key: String, compute: () -> T): T = compute()
            override fun evictAll() {}
            override fun evictByPrefix(prefix: String) {}
        }
        return GetTimelineStatsHandler(port, cache, RiotIdNormalizer(aliases))
    }

    @Test
    fun `포지션 요약은 라인 순서로 나오고 라인 이긴 쪽 승률을 센다`() {
        val result = handler().getTimelineStats("normal")

        assertEquals(positions.map { it.name }, result.positions.map { it.position })
        val top = result.positions.first().stats
        assertEquals(2, top.games)
        assertEquals(1, top.leadGames, "앞선 사람은 블루 탑 한 명")
        assertEquals(100.0, top.leadWinRate)
        // 나머지 라인은 동률이라 앞선 쪽이 없다.
        assertNull(result.positions[1].stats.leadWinRate)
    }

    @Test
    fun `라인별 결과는 그 라인에서 뛴 사람만 담는다`() {
        val result = handler().getTimelineLane("top", "normal")

        assertEquals("TOP", result.position)
        assertEquals(listOf("p1#KR1", "p6#KR1"), result.players.map { it.riotId })
        assertEquals(1_500.0, result.players.first().stats.avgGoldDiff15)
        assertEquals("TOP", result.players.first().position)
    }

    @Test
    fun `챔피언 필터는 대소문자를 가리지 않고 포지션별로도 쪼갠다`() {
        val result = handler().getTimelineChampions("normal", "aatrox")

        val aatrox = result.champions.single()
        assertEquals("Aatrox", aatrox.champion)
        assertEquals(1_500.0, aatrox.stats.avgGoldDiff15)
        assertEquals(listOf("TOP"), aatrox.byPosition.map { it.position })
        assertEquals(10, handler().getTimelineChampions("normal", null).champions.size)
    }

    @Test
    fun `개인 결과는 별명을 합친 이름으로 찾고 포지션 챔피언별로 쪼갠다`() {
        val result = handler(aliases = mapOf("p1#KR1" to "본캐#KR1")).getPlayerTimeline("p1#KR1")

        assertEquals("본캐#KR1", result.riotId)
        assertEquals(1, result.summary?.games)
        assertEquals(1, result.goldDiffRank)
        assertEquals(listOf("TOP"), result.byPosition.map { it.position })
        assertEquals(listOf("Aatrox"), result.byChampion.map { it.champion })
    }
}
