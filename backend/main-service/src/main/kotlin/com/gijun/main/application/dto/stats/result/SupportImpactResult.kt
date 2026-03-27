package com.gijun.main.application.dto.stats.result

data class SupportImpactEntry(
    val riotId: String,
    val games: Int,
    val avgHealShare: Double,
    val avgCcShare: Double,
    val avgVisionShare: Double,
    val avgShieldProxy: Double,
    val supportImpact: Double,
    val roleTag: String,
    val topChampion: String?,
    val topChampionId: Int?,
)

data class SupportImpactResult(
    val rankings: List<SupportImpactEntry>,
)
