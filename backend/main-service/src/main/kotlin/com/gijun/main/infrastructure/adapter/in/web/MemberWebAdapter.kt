package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.main.application.dto.member.command.RegisterBulkCommand
import com.gijun.main.application.dto.member.command.RegisterMemberCommand
import com.gijun.main.application.dto.member.result.BulkRegisterResult
import com.gijun.main.application.dto.member.result.MemberResult
import com.gijun.main.application.port.`in`.DeleteMemberUseCase
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.`in`.RegisterMemberUseCase
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Member", description = "내전 멤버 관리 API")
@RestController
@RequestMapping("/api/members")
class MemberWebAdapter(
    private val registerMemberUseCase: RegisterMemberUseCase,
    private val getMembersUseCase: GetMembersUseCase,
    private val deleteMemberUseCase: DeleteMemberUseCase,
) {
    @Operation(summary = "멤버 목록 조회")
    @GetMapping
    fun getAll(): CommonApiResponse<List<MemberResult>> = CommonApiResponse.success(getMembersUseCase.getAll())

    @Operation(summary = "멤버 등록", description = "Riot ID(게임이름#태그)로 멤버를 등록합니다. PUUID는 Riot API에서 자동 조회합니다")
    @PostMapping("/register")
    fun register(
        @RequestBody command: RegisterMemberCommand,
    ): CommonApiResponse<MemberResult> = CommonApiResponse.success(registerMemberUseCase.register(command))

    @Operation(summary = "멤버 일괄 등록", description = "여러 Riot ID를 한 번에 등록합니다")
    @PostMapping("/register-bulk")
    fun registerBulk(
        @RequestBody command: RegisterBulkCommand,
    ): CommonApiResponse<BulkRegisterResult> = CommonApiResponse.success(registerMemberUseCase.registerBulk(command))

    @Operation(summary = "멤버 삭제")
    @DeleteMapping("/{puuid}")
    fun delete(
        @Parameter(description = "삭제할 멤버의 PUUID")
        @PathVariable puuid: String,
    ): CommonApiResponse<Unit> {
        deleteMemberUseCase.delete(puuid)
        return CommonApiResponse.success(Unit)
    }
}
