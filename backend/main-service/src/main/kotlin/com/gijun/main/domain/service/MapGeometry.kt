package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MapPoint
import com.gijun.main.domain.model.match.Position
import kotlin.math.abs
import kotlin.math.hypot

/** 소환사의 협곡의 세 라인. [Position] 다섯 자리와 달리 봇은 원딜·서폿이 같이 쓴다. */
enum class Lane { TOP, MID, BOT }

/** 미드 대각선을 기준으로 맵의 위쪽(바론 쪽)과 아래쪽(드래곤 쪽). */
enum class MapSide { TOP_SIDE, BOT_SIDE }

/**
 * 좌표 하나가 속한 영역. 판정 우선순위는 [MapGeometry.regionOf] 가 고정한다.
 *
 * 정글은 진영(블루/레드) × 쪽(위/아래) 네 조각으로 나눈다 — 카운터 정글을 세려면
 * "누구 정글인가"가 필요하다.
 */
enum class MapRegion {
    BLUE_BASE, RED_BASE,
    TOP_LANE, MID_LANE, BOT_LANE,
    RIVER,
    BLUE_TOP_JUNGLE, BLUE_BOT_JUNGLE, RED_TOP_JUNGLE, RED_BOT_JUNGLE,
}

/**
 * 맵 좌표를 영역으로 옮기는 순수 기하. 지표로 무엇을 부를지는 [PositionMetrics] 가 정한다.
 *
 * ## 상수는 전부 저장된 경기에서 실측했다
 *
 * 통설로 도는 숫자(맵 한계 14,870 등)를 쓰지 않았다. 아래 값은 17판의 `BUILDING_KILL` 244건과
 * `ELITE_MONSTER_KILL` 153건을 **파괴당한 팀별로 갈라** 평균한 결과다. 팀을 합쳐 평균하면
 * 라인이 휘는 구간에서 두 포탑의 중점이 라인 밖으로 떨어져 쓸 수 없다 — 한 번 그렇게 뽑았다가
 * 버렸다.
 *
 * ```
 * 블루 탑  외곽(  981,10441) 내부( 1512, 6699) 기지( 1169, 4287)
 * 레드 탑  외곽( 4318,13875) 내부( 7943,13411) 기지(10481,13650) 억제기(11275,13657)
 * 블루 미드 외곽( 5790, 6304) 내부( 4961, 4743) 기지( 3651, 3696) 억제기( 3199, 3215) 쌍둥이( 1992, 2066)
 * 레드 미드 외곽( 8858, 8443) 내부( 9682, 9973) 기지(11134,11207) 억제기(10961,10961) 쌍둥이(12625,12620)
 * 블루 봇  외곽(10504, 1029) 내부( 6919, 1483) 기지( 4281, 1253) 억제기( 3468, 1230)
 * 레드 봇  외곽(13866, 4505) 내부(13327, 8226) 기지(13624,10572) 억제기(13599,11319)
 * 바론·전령·유충 둥지 ≈ (4,900, 10,300)      드래곤 둥지 ≈ (9,910, 4,530)
 * 좌표 실측 범위 x 130~14,589 / y 135~14,673  (0,0) 은 0건)
 * ```
 *
 * ## 해상도에 대한 주의
 *
 * 여기 있는 함수는 좌표 하나를 받아 영역을 돌려주는 것뿐이다. 그 좌표가 얼마나 믿을 만한지는
 * 출처에 달렸다 — **이벤트 좌표는 정확하고, 프레임 좌표는 분당 1점**이다. 이 차이를 어떻게
 * 다루는지는 [PositionMetrics] 의 머리 주석에 적어 뒀다.
 */
object MapGeometry {

    /** 통설상의 맵 한계. 좌표를 화면 비율로 정규화할 때만 쓴다 — 영역 판정에는 쓰지 않는다. */
    const val MAP_MAX = 14_870

    /**
     * 진영 경계. `x + y` 가 이 값보다 작으면 블루 진영이다.
     *
     * 맵은 미드 대각선이 아니라 **이 반대각선**을 기준으로 두 진영이 갈린다. 값은 양 팀 쌍둥이
     * 포탑의 중점 (7,308, 7,343) 에서 나왔다 — 합 14,651. 통설의 14,870 을 쓰면 경계가
     * 레드 쪽으로 220 밀려 "상대 진영 체류" 가 블루에게 유리하게 편향된다.
     */
    const val TEAM_HALF_SUM = 14_650

    /**
     * 라인 회랑의 반폭. 라인 중심선에서 이만큼 안쪽이면 그 라인에 있다고 본다.
     *
     * 측정된 건물 15개가 아래 폴리라인에서 최대 571 떨어져 있고, 라인 역할의 라인전 구간
     * 평균 좌표도 1,000 안쪽이다. 여유를 둬 1,400 으로 잡았다 — 좁히면 라인에 서 있는 사람이
     * 로밍으로 세지고, 넓히면 정글이 라인에 먹힌다.
     */
    const val LANE_HALF_WIDTH = 1_400.0

    /**
     * 강 띠의 반폭. `x + y` 가 [TEAM_HALF_SUM] 에서 이만큼 안쪽이면 강이다(라인이 아닐 때).
     *
     * 합 기준이라 실제 수직 거리로는 약 990 이다. 바론 둥지가 중심에서 +478, 드래곤 둥지가
     * −210 이라 둘 다 이 띠에 들어온다 — 강을 반대각선 띠로 근사한 근거다.
     */
    const val RIVER_HALF_SUM = 1_400

    /**
     * 기지 반경. 쌍둥이 포탑을 중심으로 이만큼 안쪽이면 기지다.
     *
     * 2,600 이면 기지 포탑(최대 2,429)과 억제기(1,666)는 안에, 내부 포탑(3,998)은 밖에 떨어진다.
     * 죽어서 분수에 있는 프레임을 걸러내는 게 이 영역의 주 용도다.
     */
    const val BASE_RADIUS = 2_600.0

    val BLUE_NEXUS = MapPoint(1_992, 2_066)
    val RED_NEXUS = MapPoint(12_625, 12_620)

    /**
     * 라인 중심선. 쌍둥이 포탑에서 시작해 반대편 쌍둥이 포탑까지 이은 꺾은선이다.
     *
     * 미드는 직선 하나로 충분하다(측정 건물 5개가 192~332 안). 탑·봇은 모서리에서 꺾이므로
     * 중간 점을 둬야 한다 — 직선으로 이으면 모서리 구간이 통째로 정글이 된다.
     */
    private val LANE_PATHS: Map<Lane, List<MapPoint>> = mapOf(
        Lane.TOP to listOf(
            MapPoint(1_900, 2_100), MapPoint(1_250, 11_000),
            MapPoint(2_400, 13_200), MapPoint(11_300, 13_700), RED_NEXUS,
        ),
        Lane.MID to listOf(BLUE_NEXUS, RED_NEXUS),
        Lane.BOT to listOf(
            MapPoint(2_100, 1_900), MapPoint(11_000, 1_250),
            MapPoint(13_200, 2_400), MapPoint(13_700, 11_300), RED_NEXUS,
        ),
    )

    // ────────── 판정 ──────────

    /** 이 좌표가 어느 팀 진영인지. 100(블루) 또는 200(레드). */
    fun halfOf(p: MapPoint): Int = if (p.x + p.y < TEAM_HALF_SUM) 100 else 200

    /** 미드 대각선 기준으로 위(바론 쪽)인지 아래(드래곤 쪽)인지. */
    fun sideOf(p: MapPoint): MapSide {
        val dx = (RED_NEXUS.x - BLUE_NEXUS.x).toLong()
        val dy = (RED_NEXUS.y - BLUE_NEXUS.y).toLong()
        val cross = dx * (p.y - BLUE_NEXUS.y) - dy * (p.x - BLUE_NEXUS.x)
        return if (cross > 0) MapSide.TOP_SIDE else MapSide.BOT_SIDE
    }

    /** 라인 중심선까지의 최단 거리. */
    fun distanceToLane(p: MapPoint, lane: Lane): Double =
        LANE_PATHS.getValue(lane).zipWithNext()
            .minOf { (a, b) -> distanceToSegment(p, a, b) }

    /** 회랑 안이면 그 라인, 어느 라인에도 못 들면 null. 겹치면 더 가까운 쪽. */
    fun nearestLane(p: MapPoint): Lane? =
        Lane.entries.map { it to distanceToLane(p, it) }
            .filter { it.second <= LANE_HALF_WIDTH }
            .minByOrNull { it.second }?.first

    /**
     * 영역 판정. **우선순위는 이 순서로 고정이다** — 기지 → 라인 → 강 → 정글.
     *
     * 우연한 조건 순서가 판정을 바꾸면 안 되니 함수 하나에 몰아 둔다. 기지가 라인보다 먼저인
     * 이유는 라인이 쌍둥이 포탑에서 시작해 기지를 지나기 때문이고, 라인이 강보다 먼저인 이유는
     * 미드와 강이 맵 중앙에서 교차하기 때문이다.
     */
    fun regionOf(p: MapPoint): MapRegion {
        if (distance(p, BLUE_NEXUS) <= BASE_RADIUS) return MapRegion.BLUE_BASE
        if (distance(p, RED_NEXUS) <= BASE_RADIUS) return MapRegion.RED_BASE

        when (nearestLane(p)) {
            Lane.TOP -> return MapRegion.TOP_LANE
            Lane.MID -> return MapRegion.MID_LANE
            Lane.BOT -> return MapRegion.BOT_LANE
            null -> Unit
        }

        if (abs(p.x + p.y - TEAM_HALF_SUM) <= RIVER_HALF_SUM) return MapRegion.RIVER

        val blue = halfOf(p) == 100
        return if (sideOf(p) == MapSide.TOP_SIDE) {
            if (blue) MapRegion.BLUE_TOP_JUNGLE else MapRegion.RED_TOP_JUNGLE
        } else {
            if (blue) MapRegion.BLUE_BOT_JUNGLE else MapRegion.RED_BOT_JUNGLE
        }
    }

    /** 이 영역이 정글인가. 카운터 정글 판정에 쓴다. */
    fun isJungle(region: MapRegion): Boolean = region in JUNGLES

    /**
     * 맡은 자리가 서는 라인. 정글은 라인이 없어 null 이다 —
     * 라인 점유·이탈 지표를 정글러에게 매기지 않는 근거가 이것이다.
     */
    fun laneOf(position: Position): Lane? = when (position) {
        Position.TOP -> Lane.TOP
        Position.MID -> Lane.MID
        Position.ADC, Position.SUPPORT -> Lane.BOT
        Position.JUNGLE, Position.UNKNOWN -> null
    }

    // ────────── 기하 ──────────

    private val JUNGLES = setOf(
        MapRegion.BLUE_TOP_JUNGLE, MapRegion.BLUE_BOT_JUNGLE,
        MapRegion.RED_TOP_JUNGLE, MapRegion.RED_BOT_JUNGLE,
    )

    fun distance(a: MapPoint, b: MapPoint): Double =
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())

    /** 점에서 선분까지의 최단 거리. 선분 밖으로 떨어지면 가까운 끝점까지의 거리다. */
    private fun distanceToSegment(p: MapPoint, a: MapPoint, b: MapPoint): Double {
        val dx = (b.x - a.x).toDouble()
        val dy = (b.y - a.y).toDouble()
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) return distance(p, a)

        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    }
}
