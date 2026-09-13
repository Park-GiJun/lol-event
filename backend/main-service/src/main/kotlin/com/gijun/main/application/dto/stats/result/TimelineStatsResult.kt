package com.gijun.main.application.dto.stats.result

/*
 * 타임라인 기반 지표.
 *
 * 비율 필드(…Rate)는 전부 0~100 이다. 평균값은 소수 첫째 자리까지 반올림한다.
 * 타임라인은 새 수집기로 받은 경기에만 있어서 표본이 전체 경기보다 훨씬 적다 —
 * 화면은 항상 경기 수를 같이 보여줘야 한다.
 *
 * 선수·포지션·챔피언 어느 단위로 묶든 같은 [TimelineAverages] 를 쓴다. 단위마다 필드가 달라지면
 * 화면이 표마다 따로 놀기 때문이다.
 */

/** 한 묶음(선수, 포지션, 챔피언 …)의 타임라인 경기 평균. */
data class TimelineAverages(
    /** 타임라인이 있는 경기 수. */
    val games: Int,
    /** 그중 라인 상대가 있어 격차를 잴 수 있었던 경기 수. */
    val laneGames: Int,
    val winRate: Double,
    /** 격차는 전부 "나 − 같은 자리 상대"다. */
    val avgGoldDiff15: Double?,
    val avgCsDiff15: Double?,
    val avgXpDiff15: Double?,
    /** 15분 골드가 라인 상대보다 앞선 경기 비율. */
    val laneLeadRate: Double?,
    /** 15분에 라인 상대보다 앞섰던 경기의 승률. "라인을 이기면 게임도 이기나". */
    val leadWinRate: Double?,
    val leadGames: Int,
    val avgCsAt10: Double?,
    val avgGoldAt15: Double?,
    val avgEarlyKills: Double,
    val avgEarlyDeaths: Double,
    val avgEarlyAssists: Double,
    val avgSoloKills: Double,
    val firstBloodRate: Double,
    /** 죽은 경기만 평균낸다. 한 번도 안 죽었으면 null. */
    val avgFirstDeathMinute: Double?,
)

data class TimelinePlayerEntry(
    val riotId: String,
    /** 전체 표에서는 가장 많이 선 포지션, 라인별 표에서는 그 라인. */
    val position: String?,
    val stats: TimelineAverages,
)

data class TimelinePositionEntry(
    val position: String,
    val stats: TimelineAverages,
)

data class TimelineChampionEntry(
    val champion: String,
    val championId: Int,
    val stats: TimelineAverages,
    /** 포지션별로 쪼갠 것. 경기 수 내림차순. */
    val byPosition: List<TimelinePositionEntry>,
)

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
    /** TOP → SUPPORT 순. 라인 평균이라 격차는 0 근처다 — 절댓값 지표를 본다. */
    val positions: List<TimelinePositionEntry>,
)

data class TimelineLaneResult(
    val position: String,
    /** 그 라인 전체 평균. 경기가 없으면 null. */
    val summary: TimelineAverages?,
    /** 그 라인에서 뛴 경기만 센 선수별 평균. */
    val players: List<TimelinePlayerEntry>,
)

data class TimelineChampionsResult(
    val games: Int,
    /** 경기 수 내림차순. */
    val champions: List<TimelineChampionEntry>,
)

data class PlayerTimelineResult(
    val riotId: String,
    /** 타임라인 경기가 없으면 null. */
    val summary: TimelineAverages?,
    /** 15분 골드 격차 순위. 라인 경기가 없으면 null. */
    val goldDiffRank: Int?,
    val rankedPlayers: Int,
    val byPosition: List<TimelinePositionEntry>,
    val byChampion: List<TimelineChampionEntry>,
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
