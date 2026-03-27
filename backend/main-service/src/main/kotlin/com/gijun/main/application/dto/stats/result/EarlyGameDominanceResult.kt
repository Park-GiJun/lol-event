package com.gijun.main.application.dto.stats.result

data class EarlyGameDominanceEntry(
    val riotId: String,
    val games: Int,
    val firstBloodRate: Double,
    val firstTowerRate: Double,
    val earlyGameScore: Double,
    val firstBloodWinRate: Int,
    val noFirstBloodWinRate: Int,
    val badges: List<String>,
)

data class EarlyGameDominanceResult(
    val rankings: List<EarlyGameDominanceEntry>,
    val firstBloodKing: String?,
    val towerDestroyer: String?,
    val overallFirstBloodWinRate: Double,
    val overallFirstTowerWinRate: Double,
)
