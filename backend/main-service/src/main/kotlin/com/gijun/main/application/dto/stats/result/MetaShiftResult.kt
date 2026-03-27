package com.gijun.main.application.dto.stats.result

data class MetaShiftChampion(
    val champion: String,
    val championId: Int,
    val totalGames: Int,
    val pickRate: Double,
    val recentPickRate: Double,
    val olderPickRate: Double,
    val trend: Double,
    val winRate: Double,
    val metaTag: String,
)

data class MetaShiftResult(
    val risingChampions: List<MetaShiftChampion>,
    val fallingChampions: List<MetaShiftChampion>,
    val stableTopChampions: List<MetaShiftChampion>,
    val totalMatchesAnalyzed: Int,
)
