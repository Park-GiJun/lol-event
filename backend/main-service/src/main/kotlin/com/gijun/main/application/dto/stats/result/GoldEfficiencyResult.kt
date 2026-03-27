package com.gijun.main.application.dto.stats.result

data class GoldEfficiencyEntry(
    val riotId: String,
    val games: Int,
    val avgDmgPerGold: Double,
    val avgVisionPerGold: Double,
    val avgObjPerGold: Double,
    val avgCsPerGold: Double,
    val goldEfficiencyScore: Double,
    val tags: List<String>,
)

data class GoldEfficiencyResult(
    val rankings: List<GoldEfficiencyEntry>,
    val dmgEfficiencyKing: String?,
    val visionEfficiencyKing: String?,
    val csEfficiencyKing: String?,
    val objEfficiencyKing: String?,
)
