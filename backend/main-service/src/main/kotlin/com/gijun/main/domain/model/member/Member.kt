package com.gijun.main.domain.model.member

import java.time.LocalDateTime

data class Member(
    val id: Long = 0,
    val riotId: String,
    val puuid: String,
    val registeredAt: LocalDateTime = LocalDateTime.now()
)
