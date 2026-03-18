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

data class LaneStat(
    val position: String,          // TOP / JUNGLE / MID / BOTTOM / SUPPORT
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
    val avgVisionScore: Double,
    val avgDamageTaken: Int,       // 탑/서폿 강조
    val avgObjectiveDamage: Int,   // 정글 강조
    val avgWardsPlaced: Double,    // 서폿 강조
    val avgCcTime: Double,         // 서폿 강조 (timeCCingOthers)
    val avgNeutralMinions: Double, // 정글 강조
)

data class PlayerLaneStat(
    val riotId: String,
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
    val avgVisionScore: Double,
    val avgDamageTaken: Int,
    val avgObjectiveDamage: Int,
    val avgWardsPlaced: Double,
    val avgCcTime: Double,
    val avgNeutralMinions: Double,
    val topChampion: String?,
    val topChampionId: Int?,
)

data class LaneLeaderboardResult(
    val lane: String,
    val players: List<PlayerLaneStat>,
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
    val laneStats: List<LaneStat> = emptyList(),
)
