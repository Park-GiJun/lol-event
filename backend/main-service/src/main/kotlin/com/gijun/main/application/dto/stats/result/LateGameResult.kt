package com.gijun.main.application.dto.stats.result

data class LateGamePlayerEntry(
    val riotId: String,
    val games: Int,
    val avgInhibitorKills: Double,
    val firstInhibitorRate: Double,
    val avgChampLevel: Double,
    val avgLongestTimeSpentLiving: Int,
    val avgLargestKillingSpree: Double,
    val avgLargestMultiKill: Double,
    val lateGameScore: Double,
)

data class LateGameResult(
    val players: List<LateGamePlayerEntry>,
)
