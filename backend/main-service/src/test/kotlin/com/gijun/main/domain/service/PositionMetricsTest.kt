package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MapPoint
import com.gijun.main.domain.model.match.MatchTimeline
import com.gijun.main.domain.model.match.ParticipantFrame
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.model.match.TimelineEvent
import com.gijun.main.domain.model.match.TimelineFrame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PositionMetricsTest {
    // ────────── 픽스처 (좌표는 MapGeometryTest 와 같은 실측값) ──────────

    private val blueTopLane = MapPoint(1_512, 6_699) // 블루 탑 내부 포탑
    private val redTopLane = MapPoint(7_943, 13_411) // 레드 탑 내부 포탑 — 상대 진영
    private val blueMidLane = MapPoint(4_961, 4_743) // 블루 미드 내부 포탑
    private val blueBase = MapPoint(1_992, 2_066) // 블루 쌍둥이 포탑
    private val redJungleTop = MapPoint(6_500, 11_500) // 레드 위쪽 정글
    private val blueJungleTop = MapPoint(3_500, 8_500) // 블루 위쪽 정글

    private val roster =
        mapOf(
            1 to PositionMetrics.Slot(100, Position.TOP),
            2 to PositionMetrics.Slot(100, Position.JUNGLE),
            3 to PositionMetrics.Slot(100, Position.MID),
            6 to PositionMetrics.Slot(200, Position.TOP),
        )

    /** [places] 는 `분 -> (참가자 -> 좌표)`. 좌표가 null 이면 그 프레임엔 좌표가 없다. */
    private fun timeline(
        places: Map<Int, Map<Int, MapPoint?>>,
        events: List<TimelineEvent> = emptyList(),
    ) = MatchTimeline(
        frames =
            places.toSortedMap().map { (minute, byPid) ->
                TimelineFrame(
                    timestampMs = minute * 60_000L + minute * 22L, // LCU 처럼 정각을 조금 넘긴다
                    participants =
                        byPid.mapValues { (pid, at) ->
                            ParticipantFrame(participantId = pid, totalGold = 0, xp = 0, position = at)
                        },
                )
            },
        events = events,
    )

    /** 1~n 분 내내 같은 자리에 서 있는 사람 하나. */
    private fun standing(
        pid: Int,
        at: MapPoint,
        minutes: IntRange,
    ) = minutes.associateWith { mapOf(pid to at) }

    private fun kill(
        ts: Long,
        killer: Int,
        victim: Int,
        at: MapPoint?,
    ) = TimelineEvent.ChampionKill(ts, killer, victim, emptyList(), at)

    // ────────── 라인 점유 ──────────

    @Test
    fun `자기 라인에 서 있으면 라인 점유율이 백 퍼센트다`() {
        val result = PositionMetrics.of(timeline(standing(1, blueTopLane, 1..15)), roster)!!
        val top = result.of(1)!!

        assertEquals(100.0, top.laneShareRate)
        assertEquals(0.0, top.roamRate)
        assertEquals(15, top.framesSampled)
        assertEquals(15, top.lanePhaseFrames)
    }

    @Test
    fun `라인전 절반을 라인 밖에서 보내면 이탈율이 절반이다`() {
        val places =
            (1..10).associateWith { minute ->
                mapOf(1 to if (minute <= 5) blueTopLane else blueJungleTop)
            }
        val top = PositionMetrics.of(timeline(places), roster)!!.of(1)!!

        assertEquals(50.0, top.laneShareRate)
        assertEquals(50.0, top.roamRate)
    }

    @Test
    fun `자기 기지에 있는 프레임은 이탈로 세지 않는다`() {
        // 귀환은 로밍이 아니다. 라인 점유율에서만 빠진다.
        val places =
            (1..10).associateWith { minute ->
                mapOf(1 to if (minute <= 5) blueTopLane else blueBase)
            }
        val top = PositionMetrics.of(timeline(places), roster)!!.of(1)!!

        assertEquals(50.0, top.laneShareRate)
        assertEquals(0.0, top.roamRate, "기지는 라인도 아니지만 이탈도 아니다")
    }

    @Test
    fun `정글은 라인 지표를 매기지 않는다`() {
        val jungler = PositionMetrics.of(timeline(standing(2, blueJungleTop, 1..15)), roster)!!.of(2)!!

        assertNull(jungler.laneShareRate, "라인이 없는 자리에 0을 매기면 라인을 안 선다로 읽힌다")
        assertNull(jungler.roamRate)
        assertEquals(15, jungler.framesSampled, "분모는 그대로 센다")
    }

    @Test
    fun `라인전이 끝난 뒤의 프레임은 라인 지표에서 빠진다`() {
        val places =
            (1..30).associateWith { minute ->
                mapOf(1 to if (minute <= 15) blueTopLane else blueJungleTop)
            }
        val top = PositionMetrics.of(timeline(places), roster)!!.of(1)!!

        assertEquals(30, top.framesSampled)
        assertEquals(15, top.lanePhaseFrames)
        assertEquals(100.0, top.laneShareRate, "16분 이후의 이탈은 라인전 지표에 안 들어간다")
    }

    // ────────── 0분 프레임 ──────────

    @Test
    fun `0분 프레임은 버린다`() {
        // 경기 시작 직후엔 열 명 모두 분수에 있다. 분모에 넣으면 전원이 기지 체류로 세진다.
        val places = mapOf(0 to mapOf(1 to blueBase), 1 to mapOf(1 to blueTopLane))
        val top = PositionMetrics.of(timeline(places), roster)!!.of(1)!!

        assertEquals(1, top.framesSampled)
        assertEquals(100.0, top.laneShareRate)
    }

    // ────────── 진영 ──────────

    @Test
    fun `상대 진영 체류는 전체와 라인전을 따로 센다`() {
        val places =
            (1..20).associateWith { minute ->
                mapOf(1 to if (minute <= 15) blueTopLane else redTopLane)
            }
        val top = PositionMetrics.of(timeline(places), roster)!!.of(1)!!

        assertEquals(25.0, top.enemyHalfRate, "20분 중 5분을 상대 진영에서 보냈다")
        assertEquals(0.0, top.enemyHalfRateLanePhase)
    }

    @Test
    fun `레드팀 기준으로는 블루 쪽이 상대 진영이다`() {
        val red = PositionMetrics.of(timeline(standing(6, blueTopLane, 1..10)), roster)!!.of(6)!!

        assertEquals(100.0, red.enemyHalfRate)
    }

    @Test
    fun `카운터 정글은 상대 진영 정글만 센다`() {
        val places =
            (1..10).associateWith { minute ->
                mapOf(2 to if (minute <= 4) redJungleTop else blueJungleTop)
            }
        val jungler = PositionMetrics.of(timeline(places), roster)!!.of(2)!!

        assertEquals(40.0, jungler.counterJungleRate)
        assertEquals(40.0, jungler.enemyHalfRate, "이 표본에선 상대 진영 체류가 전부 정글이다")
    }

    @Test
    fun `상대 진영 라인은 카운터 정글이 아니다`() {
        val top = PositionMetrics.of(timeline(standing(1, redTopLane, 1..10)), roster)!!.of(1)!!

        assertEquals(100.0, top.enemyHalfRate)
        assertEquals(0.0, top.counterJungleRate, "라인을 밀고 있는 건 정글 침입이 아니다")
    }

    // ────────── 킬·데스 자리 ──────────

    @Test
    fun `죽은 자리와 킬이 난 자리를 영역으로 센다`() {
        val events =
            listOf(
                kill(ts = 300_000, killer = 6, victim = 1, at = blueTopLane),
                kill(ts = 400_000, killer = 6, victim = 1, at = blueTopLane),
                kill(ts = 500_000, killer = 1, victim = 6, at = blueMidLane),
            )
        val result = PositionMetrics.of(timeline(standing(1, blueTopLane, 1..10), events), roster)!!

        assertEquals(mapOf(MapRegion.TOP_LANE to 2), result.of(1)!!.deathRegions)
        assertEquals(mapOf(MapRegion.MID_LANE to 1), result.of(1)!!.killRegions)
        assertEquals(mapOf(MapRegion.MID_LANE to 1), result.of(6)!!.deathRegions)
    }

    @Test
    fun `좌표가 없는 킬은 자리 분포에서 빠진다`() {
        val events = listOf(kill(ts = 300_000, killer = 6, victim = 1, at = null))
        val result = PositionMetrics.of(timeline(standing(1, blueTopLane, 1..10), events), roster)!!

        assertTrue(result.of(1)!!.deathRegions.isEmpty())
    }

    // ────────── 팀 응집도 ──────────

    @Test
    fun `팀 응집도는 중심으로부터의 평균 거리다`() {
        val places =
            mapOf(
                1 to mapOf(1 to MapPoint(6_000, 6_000), 3 to MapPoint(8_000, 8_000)),
            )
        val spread = PositionMetrics.of(timeline(places), roster)!!.teamSpreads.single { it.teamId == 100 }

        // 중심은 (7,000, 7,000). 두 점 모두 거리 √(1000²+1000²) ≈ 1,414.
        assertEquals(1_414, spread.byMinute.getValue(1))
        assertEquals(1_414, spread.average)
    }

    @Test
    fun `기지에 있는 사람은 응집도에서 뺀다`() {
        // 죽어서 분수에 있는 사람 하나가 팀 응집도를 통째로 망가뜨리기 때문이다.
        val places =
            mapOf(
                1 to mapOf(1 to MapPoint(6_000, 6_000), 3 to MapPoint(8_000, 8_000), 2 to blueBase),
            )
        val spread = PositionMetrics.of(timeline(places), roster)!!.teamSpreads.single { it.teamId == 100 }

        assertEquals(1_414, spread.byMinute.getValue(1), "분수에 있는 3번째 사람이 값을 끌어올리지 않는다")
    }

    @Test
    fun `셀 수 있는 사람이 한 명이면 그 분은 빠진다`() {
        val spread =
            PositionMetrics
                .of(timeline(standing(1, blueTopLane, 1..3)), roster)!!
                .teamSpreads
                .single { it.teamId == 100 }

        assertTrue(spread.byMinute.isEmpty())
        assertNull(spread.average)
    }

    // ────────── 결손 ──────────

    @Test
    fun `좌표가 하나도 없으면 계산하지 않는다`() {
        val places = mapOf(1 to mapOf(1 to null), 2 to mapOf(1 to null))

        assertNull(PositionMetrics.of(timeline(places), roster), "옛 수집기로 받은 경기가 여기로 떨어진다")
        assertNull(PositionMetrics.of(TimelineParser.EMPTY, roster))
    }

    @Test
    fun `좌표가 일부만 있으면 있는 프레임만 센다`() {
        val places =
            mapOf(
                1 to mapOf(1 to blueTopLane),
                2 to mapOf(1 to null),
                3 to mapOf(1 to blueTopLane),
            )
        val top = PositionMetrics.of(timeline(places), roster)!!.of(1)!!

        assertEquals(2, top.framesSampled, "좌표 없는 프레임은 분모에서도 뺀다")
    }
}
