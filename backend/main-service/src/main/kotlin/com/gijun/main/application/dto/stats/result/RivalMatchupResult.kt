package com.gijun.main.application.dto.stats.result

data class RivalMatchupEntry(
    val player1: String,
    val player2: String,
    val games: Int,
    val player1Wins: Int,
    val player2Wins: Int,
    val player1WinRate: Int,
)

data class RivalMatchupResult(
    val rivalries: List<RivalMatchupEntry>,
    val topRivalry: RivalMatchupEntry?,
)
