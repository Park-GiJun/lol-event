package com.gijun.main.application.dto

data class ChampionCount(val champ: String, val count: Int)

data class PlayerStatsDto(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val topChampions: List<ChampionCount>
)

data class StatsResponse(val stats: List<PlayerStatsDto>, val matchCount: Long)
