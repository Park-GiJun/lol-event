package com.gijun.main.application.dto.stats.result

data class ChampionCount(val champ: String, val count: Int)

data class PlayerStatsResult(
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
    val avgGold: Int,
    val avgVisionScore: Double,
    val topChampions: List<ChampionCount>
)

data class StatsResult(val stats: List<PlayerStatsResult>, val matchCount: Long)
