package com.gijun.main.application.dto.stats.result

data class PositionBadgeEntry(
    val position: String,
    val riotId: String,
    val games: Int,
    val winRate: Double,
    val kda: Double,
    val avgDamage: Double,
    val positionScore: Double,
    val topChampion: String?,
    val topChampionId: Int?,
)

data class PositionBadgeResult(
    val topPositions: List<PositionBadgeEntry>,
    val allPositionRankings: Map<String, List<PositionBadgeEntry>>,
)
