package com.gijun.main.application.dto.stats.result

data class EloRankEntry(
    val rank: Int,
    val riotId: String,
    val elo: Double,
    val games: Int,
)

data class EloLeaderboardResult(
    val players: List<EloRankEntry>,
)
