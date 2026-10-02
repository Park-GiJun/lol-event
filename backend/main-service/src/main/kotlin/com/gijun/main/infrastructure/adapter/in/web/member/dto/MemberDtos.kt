package com.gijun.main.infrastructure.adapter.`in`.web.member.dto

import com.gijun.main.application.dto.result.MemberResult
import com.gijun.main.application.dto.result.RegisterMembersItemResult
import com.gijun.main.application.dto.result.RegisterMembersResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "MemberResult")
data class MemberResponse(
    val riotId: String,
    val puuid: String,
    val registeredAt: LocalDateTime,
) {
    companion object {
        fun from(result: MemberResult) =
            MemberResponse(
                riotId = result.riotId,
                puuid = result.puuid,
                registeredAt = result.registeredAt,
            )
    }
}

@Schema(name = "RegisterMembersItemResult")
data class RegisterMembersItemResponse(
    val riotId: String,
    val status: String,
    val reason: String?,
) {
    companion object {
        fun from(result: RegisterMembersItemResult) =
            RegisterMembersItemResponse(
                riotId = result.riotId,
                status = result.status,
                reason = result.reason,
            )
    }
}

@Schema(name = "RegisterMembersResult")
data class RegisterMembersResponse(
    val results: List<RegisterMembersItemResponse>,
    val total: Int,
) {
    companion object {
        fun from(result: RegisterMembersResult) =
            RegisterMembersResponse(
                results = result.results.map(RegisterMembersItemResponse::from),
                total = result.total,
            )
    }
}
