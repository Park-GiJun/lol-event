package com.gijun.main.application.dto.result

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

data class MvpPlayerStat(
    val riotId: String,
    val games: Int,
    /** 팀 내 최고 점수 횟수 */
    val mvpCount: Int,
    /** 경기 전체 최고 점수 횟수 */
    val aceCount: Int,
    /** MVP 달성률 (%) */
    val mvpRate: Int,
    /** 평균 MVP 점수 */
    val avgMvpScore: Double,
    /** MVP 달성 최다 챔피언 */
    val topChampion: String?,
    val topChampionId: Int?,
)

data class MvpStatsResult(
    val rankings: List<MvpPlayerStat>,
    val totalGames: Int,
)

data class KillParticipationEntry(
    val riotId: String,
    val games: Int,
    val avgKp: Double, // (kills+assists) / teamKills
    val avgKpWin: Double,
    val avgKpLoss: Double,
    val avgKills: Double,
    val avgAssists: Double,
)

data class KillParticipationResult(
    val rankings: List<KillParticipationEntry>,
    val kpKing: String?,
)
