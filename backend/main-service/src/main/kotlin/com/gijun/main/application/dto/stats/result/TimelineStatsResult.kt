package com.gijun.main.application.dto.stats.result

/*
 * 타임라인 기반 지표.
 *
 * 비율 필드(…Rate)는 전부 0~100 이다. 평균값은 소수 첫째 자리까지 반올림한다.
 * 타임라인은 새 수집기로 받은 경기에만 있어서 표본이 전체 경기보다 훨씬 적다 —
 * 화면은 항상 경기 수를 같이 보여줘야 한다.
 */

data class TimelineStatsResult(
    /** 타임라인이 있는 경기 수. */
    val games: Int,
    /** 15분 골드가 앞선 팀의 승률. 판정 가능한 경기가 없으면 null. */
    val goldLeadWinRate: Double?,
    val goldLeadGames: Int,
    /** 15분에 1,500골드 이상 뒤지고도 이긴 경기 수. */
    val comebackGames: Int,
    val avgTeamGoldGapAt15: Double,
    val players: List<TimelinePlayerEntry>,
)

data class TimelinePlayerEntry(
    val riotId: String,
    /** 타임라인이 있는 경기 수. */
    val games: Int,
    /** 그중 라인 상대가 있어 격차를 잴 수 있었던 경기 수. */
    val laneGames: Int,
    val mainPosition: String?,
    val avgGoldDiff15: Double?,
    val avgCsDiff15: Double?,
    val avgXpDiff15: Double?,
    /** 15분 골드가 라인 상대보다 앞선 경기 비율. */
    val laneLeadRate: Double?,
    val avgCsAt10: Double?,
    val avgEarlyKills: Double,
    val avgEarlyDeaths: Double,
    val avgEarlyAssists: Double,
    val avgSoloKills: Double,
    val firstBloodRate: Double,
    /** 죽은 경기만 평균낸다. 한 번도 안 죽었으면 null. */
    val avgFirstDeathMinute: Double?,
)

data class PlayerTimelineResult(
    val riotId: String,
    /** 비교용 요약. 타임라인 경기가 없으면 null. */
    val summary: TimelinePlayerEntry?,
    /** 15분 골드 격차 순위. 라인 경기가 없으면 null. */
    val goldDiffRank: Int?,
    val rankedPlayers: Int,
    /** 분별 평균 골드 격차. */
    val goldDiffCurve: List<GoldDiffPoint>,
    /** 최신순. */
    val games: List<PlayerTimelineGame>,
)

data class GoldDiffPoint(
    val minute: Int,
    val avgGoldDiff: Double,
    val games: Int,
)

data class PlayerTimelineGame(
    val matchId: String,
    val gameCreation: Long,
    val champion: String,
    val championId: Int,
    val position: String,
    val win: Boolean,
    val opponentRiotId: String?,
    val opponentChampion: String?,
    val opponentChampionId: Int?,
    val goldDiff15: Int?,
    val csDiff15: Int?,
    val xpDiff15: Int?,
    val earlyKills: Int,
    val earlyDeaths: Int,
    val earlyAssists: Int,
    val soloKills: Int,
    val goldDiffByMinute: List<Int>,
)
