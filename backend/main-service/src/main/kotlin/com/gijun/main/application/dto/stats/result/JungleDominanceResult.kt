package com.gijun.main.application.dto.stats.result

data class JungleDominanceEntry(
    val riotId: String,
    val games: Int,
    val avgInvadeRatio: Double,
    val avgObjShare: Double,
    val avgKp: Double,
    val avgJungleCs: Double,
    val avgJungleDominance: Double,
    val playStyleTag: String,
    val topChampion: String?,
    val topChampionId: Int?,
)

data class JungleDominanceResult(
    val rankings: List<JungleDominanceEntry>,
)
