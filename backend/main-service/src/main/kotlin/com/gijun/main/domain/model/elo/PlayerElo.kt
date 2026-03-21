package com.gijun.main.domain.model.elo

import java.time.LocalDateTime

data class PlayerElo(
    val id: Long = 0,
    val riotId: String,
    val elo: Double = 1500.0,
    val games: Int = 0,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
