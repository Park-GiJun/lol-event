package com.gijun.main.application.dto.command

data class RegisterMemberCommand(
    val riotId: String,
)

data class RegisterBulkCommand(
    val riotIds: List<String>,
)
