package com.gijun.main.application.dto.stats.result

data class MultiKillEvent(
    val riotId: String,
    val champion: String,
    val championId: Int,
    val multiKillType: String,  // "PENTA", "QUADRA", "TRIPLE", "DOUBLE"
    val matchId: String,
    val gameCreation: Long,
)

data class PlayerMultiKillStat(
    val riotId: String,
    val pentaKills: Int,
    val quadraKills: Int,
    val tripleKills: Int,
    val doubleKills: Int,
    val topChampion: String?,
    val topChampionId: Int?,
)

data class MultiKillHighlightsResult(
    val pentaKillEvents: List<MultiKillEvent>,      // 전체 펜타킬 이벤트 (최신순)
    val recentHighlights: List<MultiKillEvent>,     // 최근 쿼드라 이상 이벤트 20개
    val playerRankings: List<PlayerMultiKillStat>,  // 플레이어별 멀티킬 합계 (펜타킬 내림차순)
)
