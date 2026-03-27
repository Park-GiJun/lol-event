package com.gijun.main.application.dto.stats.result

data class PlayerStatSnapshot(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Double,
    val avgCs: Double,
    val avgGold: Double,
    val avgVisionScore: Double,
)

data class PlayerComparisonResult(
    val player1: String,
    val player2: String,
    val togetherGames: Int,
    val togetherWinRate: Int,
    val p1TogetherStats: PlayerStatSnapshot?,
    val p2TogetherStats: PlayerStatSnapshot?,
    val versusGames: Int,
    val player1VsWinRate: Int,
    val p1VersusStats: PlayerStatSnapshot?,
    val p2VersusStats: PlayerStatSnapshot?,
    val overallP1Stats: PlayerStatSnapshot,
    val overallP2Stats: PlayerStatSnapshot,
)
