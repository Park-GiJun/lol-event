package com.gijun.main.application.dto.stats.result

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
