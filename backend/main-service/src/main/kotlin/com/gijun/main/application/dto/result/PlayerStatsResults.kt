package com.gijun.main.application.dto.result

data class ChampionCount(
    val champ: String,
    val count: Int,
)

data class PlayerStatsResult(
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
    val topChampions: List<ChampionCount>,
)

data class StatsResult(
    val stats: List<PlayerStatsResult>,
    val matchCount: Long,
)

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
    val position: String, // TOP / JUNGLE / MID / BOTTOM / SUPPORT
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
    val avgDamageTaken: Int, // 탑/서폿 강조
    val avgObjectiveDamage: Int, // 정글 강조
    val avgWardsPlaced: Double, // 서폿 강조
    val avgCcTime: Double, // 서폿 강조 (timeCCingOthers)
    val avgNeutralMinions: Double, // 정글 강조
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
    val elo: Double = 1500.0,
    val eloRank: Int? = null,
    val championStats: List<ChampionStat>,
    val recentMatches: List<RecentMatchStat>,
    val laneStats: List<LaneStat> = emptyList(),
)

data class StreakResult(
    val riotId: String,
    /** 양수 = 연승, 음수 = 연패, 0 = 경기 없음 */
    val currentStreak: Int,
    /** "WIN", "LOSS", "NONE" */
    val currentStreakType: String,
    val longestWinStreak: Int,
    val longestLossStreak: Int,
    /** 최근 10경기 결과: "W" / "L" (최신순) */
    val recentForm: List<String>,
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
)

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

data class PlayerStatSnapshot(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Double,
    val avgCs: Double,
    val avgGold: Double,
    val avgVisionScore: Double,
)

data class PlayerComparisonResult(
    val player1: String,
    val player2: String,
    val togetherGames: Int,
    val togetherWinRate: Int,
    val p1TogetherStats: PlayerStatSnapshot?,
    val p2TogetherStats: PlayerStatSnapshot?,
    val versusGames: Int,
    val player1VsWinRate: Int,
    val p1VersusStats: PlayerStatSnapshot?,
    val p2VersusStats: PlayerStatSnapshot?,
    val overallP1Stats: PlayerStatSnapshot,
    val overallP2Stats: PlayerStatSnapshot,
)
