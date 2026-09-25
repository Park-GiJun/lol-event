package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MapPoint
import com.gijun.main.domain.model.match.MatchTimeline
import com.gijun.main.domain.model.match.TimelineEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TeamFightDetectorTest {

    // ────────── 픽스처 ──────────

    /** 1~5 블루, 6~10 레드. */
    private val teams = (1..10).associateWith { if (it <= 5) 100 else 200 }

    /** 맵 중앙 근처. 좌표 조건을 건드리지 않는 기본 자리다. */
    private val center = MapPoint(7_300, 7_300)

    /** 드래곤 둥지 — 중앙에서 충분히 멀다(약 3,900). */
    private val dragonPit = MapPoint(9_910, 4_530)

    private fun kill(
        ts: Long,
        killer: Int,
        victim: Int,
        assists: List<Int> = emptyList(),
        at: MapPoint? = center,
    ) = TimelineEvent.ChampionKill(ts, killer, victim, assists, at)

    private fun dragon(ts: Long, killer: Int = 2) =
        TimelineEvent.EliteMonsterKill(ts, killer, "DRAGON", "FIRE_DRAGON", emptyList(), dragonPit)

    private fun fights(vararg events: TimelineEvent) =
        TeamFightDetector.of(MatchTimeline(frames = emptyList(), events = events.sortedBy { it.timestampMs }), teams)

    // ────────── 시간으로 묶기 ──────────

    @Test
    fun `간격이 열 초 이내면 같은 교전이다`() {
        val result = fights(
            kill(ts = 600_000, killer = 1, victim = 6),
            kill(ts = 610_000, killer = 2, victim = 7),
        )

        assertEquals(1, result.size, "정확히 10초 간격은 아직 같은 교전이다")
        assertEquals(2, result.single().kills.size)
    }

    @Test
    fun `간격이 열 초를 넘으면 끊는다`() {
        val result = fights(
            kill(ts = 600_000, killer = 1, victim = 6),
            kill(ts = 610_001, killer = 2, victim = 7),
        )

        assertEquals(2, result.size)
    }

    @Test
    fun `서른 초를 넘기면 이어져 있어도 쪼갠다`() {
        // 8초씩 다섯 번 이어지면 시간 간격만으로는 한 덩어리가 된다. 상한이 그걸 끊는다.
        val result = fights(*(0..4).map { kill(ts = 600_000 + it * 8_000L, killer = 1, victim = 6 + it) }.toTypedArray())

        assertEquals(2, result.size, "32초째 킬은 새 교전으로 넘어간다")
        assertEquals(4, result.first().kills.size)
        assertEquals(1, result.last().kills.size)
    }

    // ────────── 좌표로 묶기 ──────────

    @Test
    fun `좌표가 멀면 시간이 붙어 있어도 다른 교전이다`() {
        val result = fights(
            kill(ts = 600_000, killer = 1, victim = 6, at = center),
            kill(ts = 602_000, killer = 2, victim = 7, at = dragonPit),
        )

        assertEquals(2, result.size, "맵 양쪽에서 동시에 벌어진 교전은 시간만으로는 못 가른다")
    }

    @Test
    fun `좌표가 없으면 시간만으로 묶는다`() {
        val result = fights(
            kill(ts = 600_000, killer = 1, victim = 6, at = null),
            kill(ts = 602_000, killer = 2, victim = 7, at = null),
        )

        assertEquals(1, result.size)
        assertNull(result.single().centroid, "좌표가 하나도 없으면 중심도 없다")
        assertNull(result.single().region)
    }

    // ────────── 득실 ──────────

    @Test
    fun `킬은 희생자의 팀을 뒤집어 센다`() {
        // 포탑이 막타를 치면 killerId 가 0 이라 킬러로는 셀 수 없다.
        val fight = fights(
            kill(ts = 600_000, killer = 1, victim = 6),
            kill(ts = 603_000, killer = 0, victim = 7),
            kill(ts = 606_000, killer = 8, victim = 2),
        ).single()

        assertEquals(2, fight.team100Kills, "막타가 포탑이어도 레드가 죽었으면 블루의 킬이다")
        assertEquals(1, fight.team200Kills)
        assertEquals(100, fight.winnerTeamId)
    }

    @Test
    fun `킬 수가 같으면 승자가 없다`() {
        val fight = fights(
            kill(ts = 600_000, killer = 1, victim = 6),
            kill(ts = 604_000, killer = 7, victim = 2),
        ).single()

        assertNull(fight.winnerTeamId, "교환으로 끝난 교전에 승자를 매기지 않는다")
    }

    @Test
    fun `먼저 킬을 낸 팀을 교전을 연 팀으로 본다`() {
        val fight = fights(
            kill(ts = 600_000, killer = 7, victim = 2),
            kill(ts = 604_000, killer = 1, victim = 6),
            kill(ts = 608_000, killer = 3, victim = 8),
        ).single()

        assertEquals(200, fight.openedByTeamId)
        assertEquals(100, fight.winnerTeamId, "열었다고 이기는 건 아니다")
    }

    @Test
    fun `참여자는 킬러 희생자 어시스트를 합치고 포탑은 뺀다`() {
        val fight = fights(
            kill(ts = 600_000, killer = 1, victim = 6, assists = listOf(2, 3)),
            kill(ts = 604_000, killer = 0, victim = 7),
        ).single()

        assertEquals(setOf(1, 2, 3, 6, 7), fight.participantIds)
    }

    // ────────── 한타 여부 ──────────

    @Test
    fun `킬 두 개는 한타가 아니고 세 개부터 한타다`() {
        val trade = fights(
            kill(ts = 600_000, killer = 1, victim = 6),
            kill(ts = 604_000, killer = 7, victim = 2),
        ).single()
        assertFalse(trade.isTeamFight, "2인 교환은 트레이드다")

        val teamfight = fights(
            kill(ts = 600_000, killer = 1, victim = 6),
            kill(ts = 604_000, killer = 2, victim = 7),
            kill(ts = 608_000, killer = 3, victim = 8),
        ).single()
        assertTrue(teamfight.isTeamFight)
    }

    @Test
    fun `솔로킬도 버리지 않고 돌려준다`() {
        val result = fights(kill(ts = 600_000, killer = 1, victim = 6))

        assertEquals(1, result.size, "어디서 물렸나 지도에 쓰이므로 거르는 건 소비처 몫이다")
        assertFalse(result.single().isTeamFight)
    }

    // ────────── 전리품 ──────────

    @Test
    fun `교전 직후 오브젝트는 그 교전의 전리품으로 붙는다`() {
        val fight = fights(
            kill(ts = 600_000, killer = 1, victim = 6),
            kill(ts = 604_000, killer = 2, victim = 7),
            kill(ts = 608_000, killer = 3, victim = 8),
            dragon(ts = 620_000),
        ).single()

        assertEquals(1, fight.objectivesAfter.size)
        assertEquals(3, fight.kills.size, "오브젝트가 킬 목록에 섞이지 않는다")
    }

    @Test
    fun `한참 뒤의 오브젝트는 붙이지 않는다`() {
        val fight = fights(
            kill(ts = 600_000, killer = 1, victim = 6),
            dragon(ts = 640_000),
        ).single()

        assertTrue(fight.objectivesAfter.isEmpty(), "30초를 넘기면 그 교전의 전리품이 아니다")
    }

    // ────────── 중심 ──────────

    @Test
    fun `중심은 킬 좌표의 평균이고 영역으로 옮겨진다`() {
        val fight = fights(
            kill(ts = 600_000, killer = 1, victim = 6, at = dragonPit),
            kill(ts = 604_000, killer = 2, victim = 7, at = MapPoint(dragonPit.x + 200, dragonPit.y - 200)),
        ).single()

        assertEquals(MapPoint(10_010, 4_430), fight.centroid)
        assertEquals(MapRegion.RIVER, fight.region, "드래곤 둥지 교전은 강에서 일어난다")
    }

    // ────────── 결손 ──────────

    @Test
    fun `킬이 없으면 교전도 없다`() {
        assertTrue(fights(dragon(ts = 600_000)).isEmpty())
        assertTrue(TeamFightDetector.of(TimelineParser.EMPTY, teams).isEmpty())
    }
}
