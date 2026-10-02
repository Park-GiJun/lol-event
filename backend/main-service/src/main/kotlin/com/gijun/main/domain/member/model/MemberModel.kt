package com.gijun.main.domain.member.model

import java.time.LocalDateTime

data class MemberModel(
    val id: Long = 0,
    val riotId: String,
    val puuid: String,
    val registeredAt: LocalDateTime = LocalDateTime.now(),
)
