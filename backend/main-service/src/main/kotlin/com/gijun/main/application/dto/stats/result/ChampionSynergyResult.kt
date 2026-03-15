package com.gijun.main.application.dto.stats.result

data class ChampionSynergy(
    val champion1: String,
    val champion1Id: Int,
    val champion2: String,
    val champion2Id: Int,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgCombinedKills: Double,
)

data class ChampionSynergyResult(
    val synergies: List<ChampionSynergy>,
    val totalGames: Int,
)
