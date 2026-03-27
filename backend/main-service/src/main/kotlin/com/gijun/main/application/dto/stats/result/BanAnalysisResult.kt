package com.gijun.main.application.dto.stats.result

data class BanEntry(
    val champion: String,
    val championId: Int,
    val banCount: Int,
    val banRate: Double,  // banCount / totalGames
)

data class BanAnalysisResult(
    val topBanned: List<BanEntry>,
    val totalGamesAnalyzed: Int,
    val mostBannedChampion: String?,
)
