package com.gijun.main.application.dto.stats.result

data class DuoStat(
    val player1: String,
    val player2: String,
    val games: Int,
    val wins: Int,
    /** 관측 승률. 항상 games 와 같이 보여줄 것. */
    val winRate: Int,
    /** 전체 평균 쪽으로 끌어당긴 승률. 정렬은 이 값으로 한다. */
    val adjustedWinRate: Double,
    /** HIGH / MEDIUM / LOW / INSUFFICIENT */
    val sampleGrade: String,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
)

data class DuoStatsResult(
    val duos: List<DuoStat>,
)
