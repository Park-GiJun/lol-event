package com.gijun.main.domain.model.elo

import java.time.LocalDateTime

data class PlayerEloHistory(
    val id: Long = 0,
    val riotId: String,
    val matchId: String,
    val eloBefore: Double,
    val eloAfter: Double,
    val delta: Double,
    val win: Boolean,
    val gameCreation: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
