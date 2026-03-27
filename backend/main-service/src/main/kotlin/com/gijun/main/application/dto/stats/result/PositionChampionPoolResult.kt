package com.gijun.main.application.dto.stats.result

data class PositionChampEntry(
    val champion: String,
    val championId: Int,
    val games: Int,
    val winRate: Double,
    val kda: Double,
)

data class PlayerPositionEntry(
    val riotId: String,
    val position: String,
    val games: Int,
    val winRate: Double,
    val topChampion: String?,
    val topChampionId: Int?,
    val champions: List<PositionChampEntry>,
)

data class PositionChampionPoolResult(
    val allPlayers: List<PlayerPositionEntry>,
)
