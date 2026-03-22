package com.gijun.main.application.port.out

data class PlayerStatsCache(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
    val topChampion: String?,
)

data class ChampionItemStatsCache(
    val itemId: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
)

interface StatsCachePersistencePort {
    fun findPlayerCacheByMode(mode: String): List<PlayerStatsCache>
    fun findChampionItemCacheByChampionAndMode(champion: String, mode: String): List<ChampionItemStatsCache>
}
