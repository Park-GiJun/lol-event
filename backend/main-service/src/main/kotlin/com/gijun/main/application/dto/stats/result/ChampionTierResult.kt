package com.gijun.main.application.dto.stats.result

data class ChampionTierEntry(
    val champion: String,
    val championId: Int,
    val tier: String,
    val tierScore: Double,
    val games: Int,
    val winRate: Int,
    val kda: Double,
    val pickRate: Double,
    val avgDamage: Double,
)

data class ChampionTierResult(
    val tierList: List<ChampionTierEntry>,
    val byTier: Map<String, List<ChampionTierEntry>>,
    val totalMatches: Int,
)
