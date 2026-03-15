package com.gijun.main.application.dto.stats.result

data class ChampionPickStat(
    val champion: String,
    val championId: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
)

data class PlayerLeaderStat(
    val riotId: String,
    val displayValue: String,
    val games: Int,
)

data class OverviewStats(
    val matchCount: Int,
    val avgGameMinutes: Double,

    // 챔피언 통계
    val topPickedChampions: List<ChampionPickStat>,
    val topWinRateChampions: List<ChampionPickStat>,  // 최소 3픽 이상

    // 플레이어 명예의 전당
    val winRateLeader: PlayerLeaderStat?,
    val kdaLeader: PlayerLeaderStat?,
    val killsLeader: PlayerLeaderStat?,
    val damageLeader: PlayerLeaderStat?,
    val goldLeader: PlayerLeaderStat?,
    val csLeader: PlayerLeaderStat?,
    val visionLeader: PlayerLeaderStat?,
    val objectiveDamageLeader: PlayerLeaderStat?,
    val turretKillsLeader: PlayerLeaderStat?,
    val pentaKillsLeader: PlayerLeaderStat?,
    val wardsLeader: PlayerLeaderStat?,
    val ccLeader: PlayerLeaderStat?,
    val mostGamesPlayed: PlayerLeaderStat?,

    // 전체 오브젝트 집계
    val totalBaronKills: Int,
    val totalDragonKills: Int,
    val totalTowerKills: Int,
    val totalRiftHeraldKills: Int,
    val totalInhibitorKills: Int,
    val totalFirstBloods: Int,
)
