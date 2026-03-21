package com.gijun.main.application.dto.stats.result

data class EloHistoryEntry(
    val matchId: String,
    val eloBefore: Double,
    val eloAfter: Double,
    val delta: Double,
    val win: Boolean,
    val gameCreation: Long,
)

data class PlayerEloHistoryResult(
    val riotId: String,
    val currentElo: Double,
    val eloRank: Int?,
    val history: List<EloHistoryEntry>,
)
