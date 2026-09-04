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
    /** 표본을 50% 쪽으로 당긴 승률. 정렬은 반드시 이 값으로 한다. */
    val adjustedWinRate: Double,
    /** HIGH / MEDIUM / LOW / INSUFFICIENT */
    val sampleGrade: String,
    val certified: Boolean,
)

data class ChampionCertificateResult(
    val certifiedMasters: List<ChampionCertEntry>,
    val topChampionMasters: Map<String, ChampionCertEntry>,
)
