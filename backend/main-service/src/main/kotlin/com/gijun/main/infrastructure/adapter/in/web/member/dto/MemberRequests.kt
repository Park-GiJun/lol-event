package com.gijun.main.infrastructure.adapter.`in`.web.member.dto

import com.gijun.main.application.dto.command.RegisterMemberCommand
import com.gijun.main.application.dto.command.RegisterMembersCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "멤버 등록 요청")
data class RegisterMemberRequest(
    @field:NotBlank
    @field:Schema(description = "Riot ID(게임이름#태그)", example = "Hide on bush#KR1")
    val riotId: String,
) {
    fun toCommand() = RegisterMemberCommand(riotId = riotId)
}

@Schema(description = "멤버 일괄 등록 요청")
data class RegisterMembersRequest(
    @field:Schema(description = "Riot ID 목록. 형식이 틀린 항목은 건너뛰고 결과에 사유를 남긴다")
    val riotIds: List<String> = emptyList(),
) {
    fun toCommand() = RegisterMembersCommand(riotIds = riotIds)
}
