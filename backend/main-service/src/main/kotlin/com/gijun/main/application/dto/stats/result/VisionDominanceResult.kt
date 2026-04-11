package com.gijun.main.application.dto.stats.result

data class VisionPlayerEntry(
    val riotId: String,
    val games: Int,
    val avgVisionScore: Double,
    val avgWardsPlaced: Double,
    val avgWardsKilled: Double,
    val avgSightWardsBought: Double,
    val avgControlWardsBought: Double,
    val wardKillRate: Double,
)

data class VisionDominanceResult(
    val players: List<VisionPlayerEntry>,
)
