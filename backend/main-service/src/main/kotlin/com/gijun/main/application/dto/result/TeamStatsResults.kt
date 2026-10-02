package com.gijun.main.application.dto.result

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

data class RivalMatchupEntry(
    val player1: String,
    val player2: String,
    val games: Int,
    val player1Wins: Int,
    val player2Wins: Int,
    val player1WinRate: Int,
    /** 표본을 50% 쪽으로 당긴 player1 승률. 정렬은 이 값으로 한다. */
    val player1AdjustedWinRate: Double,
    /** HIGH / MEDIUM / LOW / INSUFFICIENT */
    val sampleGrade: String,
)

data class RivalMatchupResult(
    val rivalries: List<RivalMatchupEntry>,
    val topRivalry: RivalMatchupEntry?,
)
