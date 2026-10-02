package com.gijun.main.application.port.out

data class PlayerStatsCache(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
    val topChampion: String?,
)

data class ChampionItemStatsCache(
    val itemId: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
)

/** 룬은 (핵심 룬, 주 계열, 보조 계열) 조합 단위로 센다. 아이템처럼 칸별로 세지 않는다. */
data class ChampionRuneStatsCache(
    val keystone: Int,
    val primaryStyle: Int,
    val subStyle: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
)

/**
 * 좌표·한타 파생 지표. 배치만 채운다.
 *
 * 15분 격차 계열을 여기 담지 않은 것은 의도적이다. 그쪽은 이미 `timeline-metrics:$mode`
 * 인메모리 캐시로 계산되고 있어서, 스냅샷에서 또 읽으면 같은 집계의 정본이 두 곳이 된다.
 * 여기 있는 것은 **live 로 낼 수 없는 것**뿐이다 — 프레임 전수를 순회해야 나오는 값들이다.
 *
 * 비율은 0~100. 정글·포지션 미상은 라인 비율이 null 이다(0 이 아니다).
 */
data class PlayerTimelineStatsCache(
    val riotId: String,
    val games: Int,
    val framesSampled: Int,
    val laneShareRate: Double?,
    val roamRate: Double?,
    val enemyHalfRate: Double,
    val counterJungleRate: Double,
    val teamfights: Int,
    val teamfightKills: Int,
    val teamfightDeaths: Int,
    val aggregatedAt: Long,
)

/** 챔피언×포지션. 합계로 저장된 값을 여기서 나눠 담는다 — 롤업 때 가중치를 잃지 않기 위해서다. */
data class ChampionTimelineStatsCache(
    val champion: String,
    val position: String,
    val games: Int,
    val framesSampled: Int,
    val laneShareRate: Double?,
    val enemyHalfRate: Double,
    val aggregatedAt: Long,
)

/** 히트맵 한 칸. 격자 좌표는 0 ~ ([HEATMAP_GRID] - 1). */
data class HeatmapCell(
    val phase: String,
    val gridX: Int,
    val gridY: Int,
    val count: Int,
)

/**
 * 히트맵 격자 한 변의 칸 수. 셀 크기는 약 465 맵단위다(협곡 한 변 14,870 / 32).
 *
 * 좌표를 그대로 쌓지 않고 격자로 접으면 행 수가 (scope, kind, phase) 조합당 최대 1,024 로
 * 상한이 잡힌다. 표본이 17판인 지금 64 로 쪼개면 거의 모든 셀이 1 카운트가 되어 의미가 없다.
 * **200판을 넘기면 재검토할 것.**
 */
const val HEATMAP_GRID = 32

/** 히트맵을 누구 기준으로 모으나. */
enum class HeatmapScope { PLAYER, CHAMPION, POSITION, GLOBAL }

enum class HeatmapKind {
    /** 죽은 자리. 이벤트 좌표라 정확하다. */
    DEATH,

    /** 킬이 난 자리. 희생자가 죽은 지점이고 킬러 위치가 아니다. */
    KILL,

    /**
     * 프레임 체류. 분당 1점이라 신뢰도가 가장 낮다 — 그래서
     * [HeatmapScope.POSITION] / [HeatmapScope.GLOBAL] 만 채운다.
     */
    PRESENCE,

    /** 오브젝트·건물이 넘어간 자리. */
    OBJECTIVE,
}

/** 경기 구간. `ALL` 은 저장하지 않는다 — 세 구간을 더하면 나온다. */
enum class HeatmapPhase {
    EARLY,
    MID,
    LATE,
    ;

    companion object {
        private const val EARLY_END_MS = 900_000L
        private const val MID_END_MS = 1_500_000L

        fun of(timestampMs: Long): HeatmapPhase =
            when {
                timestampMs < EARLY_END_MS -> EARLY
                timestampMs < MID_END_MS -> MID
                else -> LATE
            }
    }
}

interface StatsCachePersistencePort {
    fun findPlayerCacheByMode(mode: String): List<PlayerStatsCache>

    fun findChampionItemCacheByChampionAndMode(
        champion: String,
        mode: String,
    ): List<ChampionItemStatsCache>

    fun findChampionRuneCacheByChampionAndMode(
        champion: String,
        mode: String,
    ): List<ChampionRuneStatsCache>

    fun findPlayerTimelineCache(
        riotId: String,
        mode: String,
    ): PlayerTimelineStatsCache?

    fun findChampionTimelineCache(
        champion: String,
        mode: String,
    ): List<ChampionTimelineStatsCache>

    /**
     * 히트맵 한 장. **live fallback 이 없다** — 격자 집계는 요청 시 돌릴 비용이 아니다.
     * 비어 있으면 화면이 "집계 대기 중"을 띄운다.
     */
    fun findHeatmap(
        mode: String,
        scopeType: String,
        scopeKey: String,
        kind: String,
    ): List<HeatmapCell>
}
