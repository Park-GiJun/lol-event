package com.gijun.main.application.dto.stats.result

data class SurvivalIndexEntry(
    val riotId: String,
    val games: Int,
    val avgDamageTaken: Double,
    val avgSelfMitigated: Double,
    val avgMitigationRatio: Double,
    val avgTankShare: Double,
    val avgSurvivalRatio: Double,
    val avgDeaths: Double,
    val survivalIndex: Double,
)

data class SurvivalIndexResult(
    val rankings: List<SurvivalIndexEntry>,
)
