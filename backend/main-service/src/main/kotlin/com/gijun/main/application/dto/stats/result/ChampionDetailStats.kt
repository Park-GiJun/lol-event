package com.gijun.main.application.dto.stats.result

data class ChampionPlayerStat(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
)

data class ChampionDetailStats(
    val champion: String,
    val championId: Int,
    val totalGames: Int,
    val totalWins: Int,
    val winRate: Int,
    val players: List<ChampionPlayerStat>,
)
