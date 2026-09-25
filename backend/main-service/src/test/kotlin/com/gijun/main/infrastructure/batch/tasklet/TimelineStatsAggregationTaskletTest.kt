package com.gijun.main.infrastructure.batch.tasklet

import com.gijun.main.application.port.out.MatchPeriodSummary
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.PositionCount
import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.service.TimelineParser
import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.ChampionTimelineStatsCacheEntity
import com.gijun.main.application.port.out.HEATMAP_GRID
import com.gijun.main.application.port.out.HeatmapKind
import com.gijun.main.application.port.out.HeatmapScope
import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.PlayerTimelineStatsCacheEntity
import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.PositionHeatmapCacheEntity
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.ChampionTimelineStatsCacheRepository
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.PlayerTimelineStatsCacheRepository
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.PositionHeatmapCacheRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

/**
 * 타임라인 집계.
 *
 * 지표 계산 자체는 TimelineMetricsTest / PositionMetricsTest / TeamFightDetectorTest 가 고정한다.
 * 여기서는 **집계 tasklet 만의 책임**을 본다 — 청크로 읽는지, 모드마다 비우고 쓰는지,
 * 히트맵 scope 규칙을 지키는지, 칼바람에서 라인 격차가 비는지.
 */
class TimelineStatsAggregationTaskletTest {

    /** 세 리포지토리가 저장한 것을 한 통에 모으고, 꺼낼 때 타입으로 가른다. */
    private val saved = mutableListOf<Any>()
    private val deleted = mutableListOf<String>()
    private val calls = mutableListOf<String>()

    /** 원본을 몇 번 끊어 읽었는지. 청크 회귀를 잡는 카운터다. */
    private var rawQueries = 0

    private val savedPlayers get() = saved.filterIsInstance<PlayerTimelineStatsCacheEntity>()
    private val savedChampions get() = saved.filterIsInstance<ChampionTimelineStatsCacheEntity>()
    private val savedHeatmap get() = saved.filterIsInstance<PositionHeatmapCacheEntity>()

    // ────────── 가짜 리포지토리 ──────────

    @Suppress("UNCHECKED_CAST")
    private fun <T> repo(type: Class<T>, label: String): T =
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
            when (method.name) {
                "saveAll" -> {
                    val batch = (args[0] as Iterable<Any>).toList()
                    saved.addAll(batch)
                    calls += "$label.saveAll"
                    batch
                }
                "deleteAllByMode" -> {
                    deleted += args[0] as String
                    calls += "$label.deleteAllByMode"
                    null
                }
                "toString" -> "Fake$label"
                "hashCode" -> System.identityHashCode(this)
                "equals" -> false
                else -> error("테스트가 예상하지 못한 호출: ${method.name}")
            }
        } as T

    // ────────── 픽스처 ──────────

    private val positions = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

    private fun roster(): List<MatchParticipant> = (1..10).map { pid ->
        val blue = pid <= 5
        MatchParticipant(
            participantId = pid,
            riotId = "p$pid#KR1",
            champion = "Champ$pid",
            championId = pid,
            team = if (blue) "blue" else "red",
            teamId = if (blue) 100 else 200,
            win = blue,
            assignedPosition = positions[(pid - 1) % 5].name,
        )
    }

    /** 블루 탑 내부 포탑 자리. 열 명 모두 여기 세워 둔다. */
    private val atBlueTopLane = """"position":{"x":1512,"y":6699}"""

    private fun raw(events: String = ""): String {
        val frames = (0..16).joinToString(",") { m ->
            val pf = (1..10).joinToString(",") { pid ->
                val lead = if (pid == 1) m * 100 else 0
                """"$pid":{"participantId":$pid,"totalGold":${5_000 + m * 300 + lead},"xp":${m * 400},""" +
                    """"minionsKilled":${m * 6},"jungleMinionsKilled":0,$atBlueTopLane}"""
            }
            val ev = if (m == 16) events else ""
            """{"timestamp":${m * 60_000L},"participantFrames":{$pf},"events":[$ev]}"""
        }
        return """{"frames":[$frames]}"""
    }

    private fun kill(ts: Long, killer: Int, victim: Int) =
        """{"type":"CHAMPION_KILL","timestamp":$ts,"killerId":$killer,"victimId":$victim,""" +
            """"assistingParticipantIds":[],"position":{"x":7300,"y":7300}}"""

    private fun tasklet(
        matchCount: Int = 1,
        events: String = "",
        queueId: Int = 3130,
        withTimeline: Boolean = true,
    ): TimelineStatsAggregationTasklet {
        val matches = (1..matchCount).map { i ->
            Match(
                matchId = "KR_$i", queueId = queueId, gameCreation = i.toLong(), gameDuration = 1_020,
                participants = roster().toMutableList(),
            )
        }
        val port = object : MatchPersistencePort {
            override fun save(match: Match) = match
            override fun existsByMatchId(matchId: String) = true
            override fun findByMatchId(matchId: String): Match? = matches.firstOrNull { it.matchId == matchId }
            override fun findAllWithParticipants(queueIds: List<Int>) =
                if (queueId in queueIds) matches else emptyList()
            override fun findPageWithParticipants(queueIds: List<Int>, page: Int, size: Int) = matches
            override fun findPeriodSummary(queueIds: List<Int>) = MatchPeriodSummary(null, null, 0, 0)
            override fun deleteByMatchId(matchId: String) {}
            override fun countByQueueIds(queueIds: List<Int>) = matches.size.toLong()
            override fun findAllOrderedByGameCreation() = matches
            override fun findInPeriodWithParticipants(queueIds: List<Int>, fromMs: Long, untilMs: Long) = matches
            override fun updateAssignedPositions(updates: Map<Long, String>) {}
            override fun saveTimelineRaw(matchId: String, raw: String) {}
            override fun findTimelineRaw(matchIds: Collection<String>): Map<String, String> {
                rawQueries++
                return if (withTimeline) matchIds.associateWith { raw(events) } else emptyMap()
            }
            override fun updateLaneMethods(updates: Map<String, LaneMethod>) {}
            override fun findPositionCounts() = emptyList<PositionCount>()
        }
        return TimelineStatsAggregationTasklet(
            port,
            repo(PlayerTimelineStatsCacheRepository::class.java, "player"),
            repo(ChampionTimelineStatsCacheRepository::class.java, "champion"),
            repo(PositionHeatmapCacheRepository::class.java, "heatmap"),
        )
    }

    private fun players(mode: String = "normal") = savedPlayers.filter { it.mode == mode }
    private fun champions(mode: String = "normal") = savedChampions.filter { it.mode == mode }
    private fun heatmap(mode: String = "normal") = savedHeatmap.filter { it.mode == mode }

    // ────────── 청크 ──────────

    @Test
    fun `원본은 청크로 끊어 읽는다`() {
        // 한 번에 들면 500경기 × 60KB 가 파싱 중인 트리와 같은 시점에 살아 있다.
        tasklet(matchCount = TimelineParser.CHUNK_SIZE + 1).aggregate()

        // 픽스처는 내전 큐라 normal 과 all 에만 걸린다(aram 은 경기가 0건이어서 읽지 않는다).
        // 51경기면 모드마다 2번씩 = 4번.
        assertEquals(4, rawQueries, "한 번에 들지 않고 CHUNK_SIZE 로 끊어야 한다")
    }

    @Test
    fun `모드마다 비우고 쓴다`() {
        tasklet().aggregate()

        assertEquals(listOf("normal", "aram", "all"), deleted.distinct())
        // 비우기가 저장보다 먼저 와야 한다 — 뒤집히면 방금 쓴 것을 지운다.
        assertTrue(
            calls.indexOf("player.deleteAllByMode") < calls.indexOf("player.saveAll"),
            "실제 호출 순서: $calls",
        )
    }

    // ────────── 사람별 ──────────

    @Test
    fun `사람별 15분 격차를 평균으로 접는다`() {
        tasklet().aggregate()

        val top = players().single { it.riotId == "p1#KR1" }
        assertEquals(1, top.games)
        assertEquals(1, top.laneGames)
        assertEquals(0, top.avgGoldDiff15!!.compareTo("1500.0".toBigDecimal()), "분당 100씩 15분")
        assertEquals(0, top.laneLeadRate!!.compareTo("100.0".toBigDecimal()))
    }

    @Test
    fun `정글은 라인 비율이 비어 있다`() {
        tasklet().aggregate()

        val jungler = players().single { it.riotId == "p2#KR1" }
        assertNull(jungler.laneShareRate, "라인이 없는 자리에 0 을 넣으면 라인을 안 선다로 읽힌다")
        assertNull(jungler.roamRate)
        assertTrue(jungler.framesSampled > 0, "분모는 그대로 센다")
    }

    @Test
    fun `라인 역할은 라인 점유율이 채워진다`() {
        tasklet().aggregate()

        val top = players().single { it.riotId == "p1#KR1" }
        assertEquals(0, top.laneShareRate!!.compareTo("100.0".toBigDecimal()), "픽스처가 탑 라인에 세워 뒀다")
    }

    @Test
    fun `한타 킬과 데스를 사람별로 센다`() {
        val events = listOf(
            kill(ts = 960_000, killer = 1, victim = 6),
            kill(ts = 964_000, killer = 2, victim = 7),
            kill(ts = 968_000, killer = 3, victim = 8),
        ).joinToString(",")
        tasklet(events = events).aggregate()

        assertEquals(1, players().single { it.riotId == "p1#KR1" }.teamfights)
        assertEquals(1, players().single { it.riotId == "p1#KR1" }.teamfightKills)
        assertEquals(1, players().single { it.riotId == "p6#KR1" }.teamfightDeaths)
    }

    @Test
    fun `킬이 두 개뿐이면 한타로 세지 않는다`() {
        val events = listOf(
            kill(ts = 960_000, killer = 1, victim = 6),
            kill(ts = 964_000, killer = 7, victim = 2),
        ).joinToString(",")
        tasklet(events = events).aggregate()

        assertEquals(0, players().single { it.riotId == "p1#KR1" }.teamfights, "2인 교환은 트레이드다")
    }

    // ────────── 챔피언별 ──────────

    @Test
    fun `챔피언은 평균이 아니라 합계와 개수로 담는다`() {
        // 포지션별 행을 챔피언 총합으로 롤업할 때 평균은 가중치 없이 더할 수 없다.
        tasklet(matchCount = 2).aggregate()

        val row = champions().single { it.champion == "Champ1" }
        assertEquals("TOP", row.position, "포지션이 키에 들어간다")
        assertEquals(2, row.games)
        assertEquals(3_000L, row.sumGoldDiff15, "1,500 짜리 두 판")
        assertEquals(2, row.laneGames)
        assertEquals(2, row.wins)
    }

    // ────────── 히트맵 ──────────

    @Test
    fun `체류 히트맵은 사람별로 만들지 않는다`() {
        // 분당 1점 한계상 가장 못 믿을 수치인데 저장 비용은 가장 크다.
        tasklet().aggregate()

        val presence = heatmap().filter { it.kind == HeatmapKind.PRESENCE.name }
        assertTrue(presence.isNotEmpty())
        assertEquals(
            setOf(HeatmapScope.POSITION.name, HeatmapScope.GLOBAL.name),
            presence.map { it.scopeType }.toSet(),
        )
    }

    @Test
    fun `데스와 킬 히트맵은 사람별 챔피언별로 만든다`() {
        tasklet(events = kill(ts = 960_000, killer = 1, victim = 6)).aggregate()

        val deaths = heatmap().filter { it.kind == HeatmapKind.DEATH.name }
        assertTrue(deaths.any { it.scopeType == HeatmapScope.PLAYER.name && it.scopeKey == "p6#KR1" })
        assertTrue(deaths.any { it.scopeType == HeatmapScope.CHAMPION.name && it.scopeKey == "Champ6" })

        val kills = heatmap().filter { it.kind == HeatmapKind.KILL.name }
        assertTrue(kills.any { it.scopeType == HeatmapScope.PLAYER.name && it.scopeKey == "p1#KR1" })
    }

    @Test
    fun `히트맵 좌표는 격자 범위 안이다`() {
        tasklet().aggregate()

        val last = HEATMAP_GRID - 1
        assertTrue(heatmap().all { it.gridX in 0..last && it.gridY in 0..last }, "격자 밖 셀이 나오면 안 된다")
    }

    @Test
    fun `0분 프레임은 체류 히트맵에서 빠진다`() {
        // 경기 시작 직후엔 열 명 모두 자기 분수에 있다.
        tasklet().aggregate()

        val presenceCount = heatmap()
            .filter { it.kind == HeatmapKind.PRESENCE.name && it.scopeType == HeatmapScope.GLOBAL.name }
            .sumOf { it.count }
        assertEquals(160, presenceCount, "1~16분 × 10명. 0분은 빠진다")
    }

    // ────────── 결손 ──────────

    @Test
    fun `타임라인이 없으면 아무것도 쌓지 않는다`() {
        tasklet(withTimeline = false).aggregate()

        assertTrue(savedPlayers.isEmpty())
        assertTrue(savedHeatmap.isEmpty())
        assertEquals(listOf("normal", "aram", "all"), deleted.distinct(), "그래도 옛 스냅샷은 비운다")
    }

    @Test
    fun `칼바람은 라인 격차가 비어 나온다`() {
        // 라인이 없어 LaneScores 의 상대 목록이 빈 목록이다. 버그가 아니다.
        tasklet(queueId = 3270).aggregate()

        val aram = players("aram")
        assertTrue(aram.isNotEmpty(), "칼바람도 경기 수는 센다")
        assertTrue(aram.all { it.avgGoldDiff15 == null }, "라인 상대가 없으면 격차를 못 잰다")
        assertTrue(aram.all { it.laneGames == 0 })
        assertTrue(players("normal").isEmpty(), "칼바람 경기는 normal 에 들어가지 않는다")
    }
}
