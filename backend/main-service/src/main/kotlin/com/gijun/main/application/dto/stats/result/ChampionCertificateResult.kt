package com.gijun.main.application.dto.stats.result

data class ChampionCertEntry(
    val riotId: String,
    val champion: String,
    val championId: Int,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Double,
    val certified: Boolean,
)

data class ChampionCertificateResult(
    val certifiedMasters: List<ChampionCertEntry>,
    val topChampionMasters: Map<String, ChampionCertEntry>,
)
