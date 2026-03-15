package com.gijun.main.application.dto.stats.result

data class ChampionStat(
    val champion: String,
    val championId: Int,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
)

data class RecentMatchStat(
    val matchId: String,
    val champion: String,
    val championId: Int,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val damage: Int,
    val cs: Int,
    val gold: Int,
    val gameCreation: Long,
    val gameDuration: Int,
    val queueId: Int,
)

data class PlayerDetailStatsResult(
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
    val championStats: List<ChampionStat>,
    val recentMatches: List<RecentMatchStat>,
)
