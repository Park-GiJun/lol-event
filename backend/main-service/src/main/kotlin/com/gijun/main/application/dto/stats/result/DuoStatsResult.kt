package com.gijun.main.application.dto.stats.result

data class DuoStat(
    val player1: String,
    val player2: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
)

data class DuoStatsResult(
    val duos: List<DuoStat>,
)
