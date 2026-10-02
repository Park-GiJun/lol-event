package com.gijun.main.application.dto.result

data class ChampionPickStat(
    val champion: String,
    val championId: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
    val kda: Double = 0.0,
    val avgKills: Double = 0.0,
    val avgDeaths: Double = 0.0,
    val avgAssists: Double = 0.0,
    val avgDamage: Int = 0,
    val avgCs: Double = 0.0,
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
    val topWinRateChampions: List<ChampionPickStat>, // 최소 3픽 이상
    val topBannedChampions: List<ChampionPickStat>,
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
    val firstBloodLeader: PlayerLeaderStat?,
    // 전체 오브젝트 집계
    val totalBaronKills: Int,
    val totalDragonKills: Int,
    val totalTowerKills: Int,
    val totalRiftHeraldKills: Int,
    val totalInhibitorKills: Int,
    val totalFirstBloods: Int,
    val totalCs: Long,
)

data class WeeklyAwardEntry(
    val riotId: String,
    val displayValue: String, // 표시할 값 (숫자, 퍼센트, 챔피언명 등)
    val games: Int,
)

data class WeeklyAwardsResult(
    val mostDeaths: WeeklyAwardEntry?, // 단일 경기 최다 사망
    val worstKda: WeeklyAwardEntry?, // 평균 KDA 최하위 (최소 5게임)
    val highGoldLowDamage: WeeklyAwardEntry?, // 먹튀 골드왕
    val mostSurrenders: WeeklyAwardEntry?, // 항복 유발자
    val pentaKillHero: WeeklyAwardEntry?, // 펜타킬 영웅
    val loneHero: WeeklyAwardEntry?, // 그래도 난 했다
    val highestWinRate: WeeklyAwardEntry?, // 승률 1위 (최소 5게임)
    val mostGamesChampion: WeeklyAwardEntry?, // 특정 챔피언 최다 플레이
)
