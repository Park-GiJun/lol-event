package com.gijun.main.application.dto

import java.time.LocalDateTime

data class MemberDto(
    val riotId: String,
    val puuid: String,
    val registeredAt: LocalDateTime
)

data class RegisterMemberRequest(val riotId: String)
data class RegisterBulkRequest(val riotIds: List<String>)
data class BulkRegisterResult(val riotId: String, val status: String, val reason: String? = null)
data class BulkRegisterResponse(val results: List<BulkRegisterResult>, val total: Int)
