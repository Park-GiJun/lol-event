package com.gijun.main.domain.match.service

import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.match.model.MapPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 랜드마크 → 기대 영역 표로 고정한다.
 *
 * **좌표는 전부 저장된 17판 실측값이다** — 건물은 `BUILDING_KILL` 244건을 파괴당한 팀별로,
 * 둥지는 `ELITE_MONSTER_KILL` 153건을 몬스터별로 평균했다. 정글만 실측 랜드마크가 없어
 * 기하로 고른 점을 쓰고, 그 점은 주석에 밝혔다.
 */
class MapGeometryTest {
    // ────────── 픽스처 (전부 실측 좌표) ──────────

    private val blueTopOuter = MapPoint(981, 10_441)
    private val blueTopInner = MapPoint(1_512, 6_699)
    private val blueTopBase = MapPoint(1_169, 4_287)
    private val redTopOuter = MapPoint(4_318, 13_875)
    private val redTopInner = MapPoint(7_943, 13_411)
    private val redTopBase = MapPoint(10_481, 13_650)
    private val redTopInhibitor = MapPoint(11_275, 13_657)

    private val blueMidOuter = MapPoint(5_790, 6_304)
    private val blueMidInner = MapPoint(4_961, 4_743)
    private val blueMidBase = MapPoint(3_651, 3_696)
    private val blueMidInhibitor = MapPoint(3_199, 3_215)
    private val redMidOuter = MapPoint(8_858, 8_443)
    private val redMidInner = MapPoint(9_682, 9_973)
    private val redMidBase = MapPoint(11_134, 11_207)

    private val blueBotOuter = MapPoint(10_504, 1_029)
    private val blueBotInner = MapPoint(6_919, 1_483)
    private val blueBotBase = MapPoint(4_281, 1_253)
    private val redBotOuter = MapPoint(13_866, 4_505)
    private val redBotInner = MapPoint(13_327, 8_226)
    private val redBotBase = MapPoint(13_624, 10_572)

    private val baronPit = MapPoint(4_865, 10_263)
    private val dragonPit = MapPoint(9_910, 4_530)

    /** 양 팀 쌍둥이 포탑의 중점. 진영 경계가 지나는 자리다. */
    private val mapCenter = MapPoint(7_308, 7_343)

    // ────────── 라인 ──────────

    @Test
    fun `탑 라인 건물은 탑 라인으로 잡힌다`() {
        for (p in listOf(blueTopOuter, blueTopInner, redTopOuter, redTopInner)) {
            assertEquals(MapRegion.TOP_LANE, MapGeometry.regionOf(p), "탑 라인 건물 $p")
        }
    }

    @Test
    fun `미드 라인 건물은 미드 라인으로 잡힌다`() {
        for (p in listOf(blueMidOuter, blueMidInner, redMidOuter, redMidInner)) {
            assertEquals(MapRegion.MID_LANE, MapGeometry.regionOf(p), "미드 라인 건물 $p")
        }
    }

    @Test
    fun `봇 라인 건물은 봇 라인으로 잡힌다`() {
        for (p in listOf(blueBotOuter, blueBotInner, redBotOuter, redBotInner)) {
            assertEquals(MapRegion.BOT_LANE, MapGeometry.regionOf(p), "봇 라인 건물 $p")
        }
    }

    @Test
    fun `라인은 서로 섞이지 않는다`() {
        assertEquals(Lane.TOP, MapGeometry.nearestLane(blueTopOuter))
        assertEquals(Lane.MID, MapGeometry.nearestLane(blueMidOuter))
        assertEquals(Lane.BOT, MapGeometry.nearestLane(blueBotOuter))
    }

    @Test
    fun `모서리에서 꺾이는 탑 라인도 회랑 안이다`() {
        // 직선으로 이으면 이 구간이 통째로 정글로 떨어진다. 폴리라인을 쓰는 이유다.
        assertTrue(
            MapGeometry.distanceToLane(redTopOuter, Lane.TOP) <= MapGeometry.LANE_HALF_WIDTH,
            "레드 탑 외곽 포탑까지의 거리 ${MapGeometry.distanceToLane(redTopOuter, Lane.TOP)}",
        )
    }

    // ────────── 기지 ──────────

    @Test
    fun `기지 포탑과 억제기는 기지 안이다`() {
        for (p in listOf(blueTopBase, blueMidBase, blueMidInhibitor, blueBotBase)) {
            assertEquals(MapRegion.BLUE_BASE, MapGeometry.regionOf(p), "블루 기지 건물 $p")
        }
        for (p in listOf(redTopBase, redTopInhibitor, redMidBase, redBotBase)) {
            assertEquals(MapRegion.RED_BASE, MapGeometry.regionOf(p), "레드 기지 건물 $p")
        }
    }

    @Test
    fun `내부 포탑은 기지 밖이다`() {
        // 기지 반경이 기지 포탑(최대 2,429)은 품고 내부 포탑(3,958 이상)은 놓아야 한다.
        assertEquals(MapRegion.MID_LANE, MapGeometry.regionOf(blueMidInner))
        assertEquals(MapRegion.MID_LANE, MapGeometry.regionOf(redMidInner))
    }

    // ────────── 강 ──────────

    @Test
    fun `오브젝트 둥지는 강이다`() {
        assertEquals(MapRegion.RIVER, MapGeometry.regionOf(baronPit), "바론·전령·유충 둥지")
        assertEquals(MapRegion.RIVER, MapGeometry.regionOf(dragonPit), "드래곤 둥지")
    }

    @Test
    fun `바론은 위쪽 드래곤은 아래쪽이다`() {
        assertEquals(MapSide.TOP_SIDE, MapGeometry.sideOf(baronPit))
        assertEquals(MapSide.BOT_SIDE, MapGeometry.sideOf(dragonPit))
    }

    @Test
    fun `강과 미드가 겹치는 맵 중앙은 라인이 이긴다`() {
        // 판정 우선순위(기지 → 라인 → 강 → 정글)를 고정하는 테스트다.
        assertEquals(MapRegion.MID_LANE, MapGeometry.regionOf(mapCenter))
    }

    // ────────── 진영 ──────────

    @Test
    fun `진영은 반대각선으로 갈린다`() {
        assertEquals(100, MapGeometry.halfOf(blueTopOuter))
        assertEquals(100, MapGeometry.halfOf(blueBotOuter))
        assertEquals(200, MapGeometry.halfOf(redTopOuter))
        assertEquals(200, MapGeometry.halfOf(redBotOuter))
    }

    @Test
    fun `진영 경계는 맵 중앙을 지난다`() {
        assertEquals(100, MapGeometry.halfOf(MapPoint(7_000, 7_000)), "합 14,000 은 블루")
        assertEquals(200, MapGeometry.halfOf(MapPoint(7_500, 7_500)), "합 15,000 은 레드")
    }

    // ────────── 정글 ──────────

    @Test
    fun `정글은 진영과 쪽으로 네 조각이 된다`() {
        // 실측 랜드마크가 없어 기하로 고른 점이다 — 라인·강·기지 어디에도 안 드는 좌표.
        assertEquals(MapRegion.BLUE_TOP_JUNGLE, MapGeometry.regionOf(MapPoint(3_500, 8_500)))
        assertEquals(MapRegion.BLUE_BOT_JUNGLE, MapGeometry.regionOf(MapPoint(8_000, 3_000)))
        assertEquals(MapRegion.RED_TOP_JUNGLE, MapGeometry.regionOf(MapPoint(6_500, 11_500)))
        assertEquals(MapRegion.RED_BOT_JUNGLE, MapGeometry.regionOf(MapPoint(11_500, 6_500)))
    }

    @Test
    fun `정글 판정은 네 조각만 참이다`() {
        assertTrue(MapGeometry.isJungle(MapRegion.BLUE_TOP_JUNGLE))
        assertTrue(MapGeometry.isJungle(MapRegion.RED_BOT_JUNGLE))
        assertTrue(
            MapRegion.entries.filterNot(MapGeometry::isJungle).containsAll(
                listOf(MapRegion.RIVER, MapRegion.MID_LANE, MapRegion.BLUE_BASE),
            ),
        )
    }

    // ────────── 자리 → 라인 ──────────

    @Test
    fun `봇은 원딜과 서폿이 같이 쓰고 정글은 라인이 없다`() {
        assertEquals(Lane.TOP, MapGeometry.laneOf(Position.TOP))
        assertEquals(Lane.MID, MapGeometry.laneOf(Position.MID))
        assertEquals(Lane.BOT, MapGeometry.laneOf(Position.ADC))
        assertEquals(Lane.BOT, MapGeometry.laneOf(Position.SUPPORT))
        assertNull(MapGeometry.laneOf(Position.JUNGLE), "정글은 라인 점유 지표의 대상이 아니다")
        assertNull(MapGeometry.laneOf(Position.UNKNOWN))
    }
}
