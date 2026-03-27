package com.gijun.main.application.dto.stats.result

data class PlaystyleDnaEntry(
    val riotId: String,
    val games: Int,
    val aggression: Double,
    val durability: Double,
    val teamPlay: Double,
    val objectiveFocus: Double,
    val economy: Double,
    val visionControl: Double,
    val styleTag: String,
)

data class PlaystyleDnaResult(
    val players: List<PlaystyleDnaEntry>,
)
