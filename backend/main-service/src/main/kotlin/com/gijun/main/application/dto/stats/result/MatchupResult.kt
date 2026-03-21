package com.gijun.main.application.dto.stats.result

data class MatchupStat(
    val opponent: String,
    val opponentId: Int,
    val games: Int,
    val wins: Int,
    val winRate: Int,
)

data class ChampionMatchupResult(
    val champion: String,
    val championId: Int,
    val matchups: List<MatchupStat>,
)

data class ObjectiveStat(
    val objective: String,
    val label: String,
    val totalGames: Int,
    val gamesWithFirst: Int,
    val winsWithFirst: Int,
    val winRateWithFirst: Int,
    val winRateWithout: Int,
)

data class ObjectiveCorrelationResult(
    val totalGames: Int,
    val objectives: List<ObjectiveStat>,
)
