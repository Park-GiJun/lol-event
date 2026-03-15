package com.gijun.main.application.dto.member.result

import com.gijun.main.domain.model.member.Member
import java.time.LocalDateTime

data class MemberResult(
    val riotId: String,
    val puuid: String,
    val registeredAt: LocalDateTime
) {
    companion object {
        fun from(domain: Member) = MemberResult(
            riotId = domain.riotId,
            puuid = domain.puuid,
            registeredAt = domain.registeredAt
        )
    }
}

data class BulkRegisterItemResult(
    val riotId: String,
    val status: String,
    val reason: String? = null
)

data class BulkRegisterResult(
    val results: List<BulkRegisterItemResult>,
    val total: Int
)
