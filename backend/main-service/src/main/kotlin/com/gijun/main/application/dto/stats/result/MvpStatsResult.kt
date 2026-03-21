package com.gijun.main.application.dto.stats.result

data class MvpPlayerStat(
    val riotId: String,
    val games: Int,
    /** 팀 내 최고 점수 횟수 */
    val mvpCount: Int,
    /** 경기 전체 최고 점수 횟수 */
    val aceCount: Int,
    /** MVP 달성률 (%) */
    val mvpRate: Int,
    /** 평균 MVP 점수 */
    val avgMvpScore: Double,
    /** MVP 달성 최다 챔피언 */
    val topChampion: String?,
    val topChampionId: Int?,
)

data class MvpStatsResult(
    val rankings: List<MvpPlayerStat>,
    val totalGames: Int,
)
