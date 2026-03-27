package com.gijun.main.application.dto.stats.result

data class GrowthCurveEntry(
    val matchId: String,
    val gameCreation: Long,
    val champion: String,
    val win: Boolean,
    val kda: Double,
    val dmgShare: Double,
    val visionPerMin: Double,
    val csPerMin: Double,
    val rollingKda: Double,
    val rollingDmgShare: Double,
    val rollingCsPerMin: Double,
)

data class GrowthCurveResult(
    val riotId: String,
    val entries: List<GrowthCurveEntry>,
    val totalGames: Int,
    val recentAvgKda: Double,
    val overallAvgKda: Double,
    val trend: String,
)
