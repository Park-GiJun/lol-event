package com.gijun.main.infrastructure.batch.tasklet

import com.gijun.main.application.port.out.MatchPeriodSummary
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.PositionCount
import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.ChampionRuneStatsCacheEntity
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.ChampionRuneStatsCacheRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

/**
 * 룬 집계.
 *
 * 아이템과 달리 한 경기에서 조합이 하나뿐이라 참가자 한 명이 정확히 한 표다.
 * 그리고 룬 정보가 안 실려 온 경기가 섞여 있는데(수집분의 약 10%), 그 0 을 그대로
 * 세면 "룬 없음" 조합이 채택 1위로 올라온다. 그걸 막는지가 핵심이다.
 */
class ChampionRuneStatsAggregationTaskletTest {

    private val saved = mutableListOf<ChampionRuneStatsCacheEntity>()
    private val deletedModes = mutableListOf<String>()

    @Suppress("UNCHECKED_CAST")
    private val repo: ChampionRuneStatsCacheRepository = Proxy.newProxyInstance(
        ChampionRuneStatsCacheRepository::class.java.classLoader,
        arrayOf(ChampionRuneStatsCacheRepository::class.java),
    ) { _, method, args ->
        when (method.name) {
            "saveAll" -> {
                val batch = (args[0] as Iterable<ChampionRuneStatsCacheEntity>).toList()
                saved += batch
                batch
            }
            "deleteAllByMode" -> { deletedModes += args[0] as String; null }
            "toString" -> "FakeChampionRuneStatsCacheRepository"
            "hashCode" -> System.identityHashCode(this)
            "equals" -> false
            else -> error("테스트가 예상하지 못한 호출: ${method.name}")
        }
    } as ChampionRuneStatsCacheRepository

    private fun p(
        champion: String,
        win: Boolean,
        perk0: Int,
        primary: Int,
        sub: Int,
        riotId: String = "나#KR1",
    ) = MatchParticipant(
        riotId = riotId, champion = champion, team = "BLUE", win = win,
        perk0 = perk0, perkPrimaryStyle = primary, perkSubStyle = sub,
    )

    private fun tasklet(participants: List<MatchParticipant>): ChampionRuneStatsAggregationTasklet {
        val match = Match(
            matchId = "KR_1", queueId = 0, gameCreation = 1, gameDuration = 1800,
            participants = participants.toMutableList(),
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
            override fun findTimelineRaw(matchIds: Collection<String>) = emptyMap<String, String>()
            override fun updateLaneMethods(updates: Map<String, LaneMethod>) {}
            override fun findPositionCounts() = emptyList<PositionCount>()
        }
        return ChampionRuneStatsAggregationTasklet(port, repo)
    }

    /** normal 모드분만 골라 본다. 태스크릿은 normal/aram/all 을 다 도는데 셋 다 같은 매치를 받는다. */
    private fun normalRows() = saved.filter { it.mode == "normal" }

    @Test
    fun `같은 조합은 한 줄로 묶고 승률을 낸다`() {
        tasklet(listOf(
            p("Ahri", win = true,  perk0 = 8214, primary = 8200, sub = 8100),
            p("Ahri", win = false, perk0 = 8214, primary = 8200, sub = 8100),
            p("Ahri", win = true,  perk0 = 8214, primary = 8200, sub = 8100),
        )).aggregate()

        val row = normalRows().single()
        assertEquals("Ahri", row.champion)
        assertEquals(8214, row.keystone)
        assertEquals(3, row.picks)
        assertEquals(2, row.wins)
        assertEquals(66, row.winRate)
    }

    @Test
    fun `키스톤이 같아도 보조 계열이 다르면 다른 줄이다`() {
        tasklet(listOf(
            p("Ahri", win = true, perk0 = 8214, primary = 8200, sub = 8100),
            p("Ahri", win = true, perk0 = 8214, primary = 8200, sub = 8300),
        )).aggregate()

        val rows = normalRows()
        assertEquals(2, rows.size)
        assertEquals(setOf(8100, 8300), rows.map { it.subStyle }.toSet())
        assertTrue(rows.all { it.picks == 1 })
    }

    @Test
    fun `룬 정보가 없는 참가자는 세지 않는다`() {
        // perk0 == 0 은 룬이 안 실려 온 경기다. 세면 "룬 없음"이 1위로 올라온다.
        tasklet(listOf(
            p("Ahri", win = true, perk0 = 0,    primary = 0,    sub = 0),
            p("Ahri", win = true, perk0 = 0,    primary = 0,    sub = 0),
            p("Ahri", win = true, perk0 = 8214, primary = 8200, sub = 8100),
        )).aggregate()

        val rows = normalRows()
        assertEquals(1, rows.size)
        assertEquals(8214, rows.single().keystone)
        assertEquals(1, rows.single().picks)
        assertNull(rows.find { it.keystone == 0 })
    }

    @Test
    fun `주 계열만 비어 있어도 세지 않는다`() {
        // perk0 는 실려 왔는데 계열이 0 인 반쪽짜리 행도 걸러야 한다.
        tasklet(listOf(
            p("Ahri", win = true, perk0 = 8214, primary = 0, sub = 0),
        )).aggregate()

        assertTrue(normalRows().isEmpty())
    }

    @Test
    fun `챔피언이 다르면 따로 센다`() {
        tasklet(listOf(
            p("Ahri", win = true, perk0 = 8214, primary = 8200, sub = 8100),
            p("Zed",  win = true, perk0 = 8112, primary = 8100, sub = 8000),
        )).aggregate()

        assertEquals(setOf("Ahri", "Zed"), normalRows().map { it.champion }.toSet())
    }

    @Test
    fun `모드마다 기존 스냅샷을 먼저 지운다`() {
        tasklet(listOf(p("Ahri", win = true, perk0 = 8214, primary = 8200, sub = 8100))).aggregate()

        // 지우지 않으면 유니크 제약에 걸려 재집계가 통째로 실패한다.
        assertEquals(listOf("normal", "aram", "all"), deletedModes)
    }
}
