package com.gijun.main.application.dto.result

import com.gijun.main.domain.member.model.MemberModel
import java.time.LocalDateTime

data class MemberResult(
    val riotId: String,
    val puuid: String,
    val registeredAt: LocalDateTime,
) {
    companion object {
        fun from(domain: MemberModel) =
            MemberResult(
                riotId = domain.riotId,
                puuid = domain.puuid,
                registeredAt = domain.registeredAt,
            )
    }
}

data class RegisterMembersItemResult(
    val riotId: String,
    val status: String,
    val reason: String? = null,
)

data class RegisterMembersResult(
    val results: List<RegisterMembersItemResult>,
    val total: Int,
)
