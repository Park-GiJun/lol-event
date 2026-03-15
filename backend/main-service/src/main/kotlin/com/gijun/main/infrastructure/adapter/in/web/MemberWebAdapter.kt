package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.member.command.RegisterBulkCommand
import com.gijun.main.application.dto.member.command.RegisterMemberCommand
import com.gijun.main.application.dto.member.result.BulkRegisterResult
import com.gijun.main.application.dto.member.result.MemberResult
import com.gijun.main.application.port.`in`.DeleteMemberUseCase
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.`in`.RegisterMemberUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Member", description = "내전 멤버 관리 API")
@RestController
@RequestMapping("/api/members")
class MemberWebAdapter(
    private val registerMemberUseCase: RegisterMemberUseCase,
    private val getMembersUseCase: GetMembersUseCase,
    private val deleteMemberUseCase: DeleteMemberUseCase
) {
    @Operation(summary = "멤버 목록 조회")
    @GetMapping
    fun getAll(): CommonApiResponse<List<MemberResult>> =
        CommonApiResponse.success(getMembersUseCase.getAll())

    @Operation(summary = "멤버 등록", description = "Riot ID(게임이름#태그)로 멤버를 등록합니다. PUUID는 Riot API에서 자동 조회합니다")
    @PostMapping("/register")
    fun register(@RequestBody command: RegisterMemberCommand): CommonApiResponse<MemberResult> =
        CommonApiResponse.created(registerMemberUseCase.register(command))

    @Operation(summary = "멤버 일괄 등록", description = "여러 Riot ID를 한 번에 등록합니다")
    @PostMapping("/register-bulk")
    fun registerBulk(@RequestBody command: RegisterBulkCommand): CommonApiResponse<BulkRegisterResult> =
        CommonApiResponse.success(registerMemberUseCase.registerBulk(command))

    @Operation(summary = "멤버 삭제")
    @DeleteMapping("/{puuid}")
    fun delete(
        @Parameter(description = "삭제할 멤버의 PUUID")
        @PathVariable puuid: String
    ): CommonApiResponse<Unit> {
        deleteMemberUseCase.delete(puuid)
        return CommonApiResponse.success(Unit)
    }
}
