package com.gijun.main.domain.model.elo

import java.time.LocalDateTime

data class PlayerElo(
    val id: Long = 0,
    val riotId: String,
    val elo: Double = 1000.0,
    val games: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winStreak: Int = 0,
    val lossStreak: Int = 0,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
