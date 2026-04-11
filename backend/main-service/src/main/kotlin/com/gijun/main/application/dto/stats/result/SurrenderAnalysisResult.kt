package com.gijun.main.application.dto.stats.result

data class SurrenderPlayerEntry(
    val riotId: String,
    val games: Int,
    val surrenderGames: Int,
    val earlySurrenderGames: Int,
    val causedEarlySurrenderGames: Int,
    val surrenderRate: Double,
    val earlySurrenderRate: Double,
)

data class SurrenderAnalysisResult(
    val totalGames: Int,
    val surrenderGames: Int,
    val earlySurrenderGames: Int,
    val overallSurrenderRate: Double,
    val overallEarlySurrenderRate: Double,
    val players: List<SurrenderPlayerEntry>,
)
