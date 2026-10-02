package com.gijun.main.infrastructure.adapter.`in`.web.member

import com.gijun.main.application.port.`in`.DeleteMemberUseCase
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.`in`.RegisterMemberUseCase
import com.gijun.main.application.port.`in`.RegisterMembersUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.member.dto.MemberResponse
import com.gijun.main.infrastructure.adapter.`in`.web.member.dto.RegisterMemberRequest
import com.gijun.main.infrastructure.adapter.`in`.web.member.dto.RegisterMembersRequest
import com.gijun.main.infrastructure.adapter.`in`.web.member.dto.RegisterMembersResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import com.gijun.main.shared.infrastructure.web.common.puuidOf
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 내전 멤버.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/members` | 목록 |
 * | POST | `/api/members/register` | 한 명 등록 |
 * | POST | `/api/members/register-bulk` | 여러 명 등록 |
 * | DELETE | `/api/members/{puuid}` | 삭제 |
 * ```
 */
@RestController
@RequestMapping("/api/members", version = "1.0")
@Tag(name = "Member", description = "내전 멤버 관리 API")
class MemberWebAdapter(
    private val getMembersUseCase: GetMembersUseCase,
    private val registerMemberUseCase: RegisterMemberUseCase,
    private val registerMembersUseCase: RegisterMembersUseCase,
    private val deleteMemberUseCase: DeleteMemberUseCase,
) {
    // ===== 조회 =====

    @GetMapping
    @Operation(summary = "멤버 목록 조회")
    fun getMembers(): CommonApiResponse<List<MemberResponse>> =
        CommonApiResponse.success(getMembersUseCase.getMembers().map(MemberResponse::from))

    // ===== 변경 =====

    @PostMapping("/register")
    @Operation(summary = "멤버 등록", description = "Riot ID(게임이름#태그)로 멤버를 등록합니다. PUUID는 Riot API에서 자동 조회합니다")
    fun registerMember(
        @Valid @RequestBody request: RegisterMemberRequest,
    ): CommonApiResponse<MemberResponse> =
        CommonApiResponse.success(MemberResponse.from(registerMemberUseCase.registerMember(request.toCommand())))

    @PostMapping("/register-bulk")
    @Operation(
        summary = "멤버 일괄 등록",
        description = "여러 Riot ID를 한 번에 등록합니다. 한 건이 실패해도 나머지는 계속하고 건별 결과를 돌려줍니다",
    )
    fun registerMembers(
        @Valid @RequestBody request: RegisterMembersRequest,
    ): CommonApiResponse<RegisterMembersResponse> =
        CommonApiResponse.success(RegisterMembersResponse.from(registerMembersUseCase.registerMembers(request.toCommand())))

    @DeleteMapping("/{puuid}")
    @Operation(summary = "멤버 삭제")
    fun deleteMember(
        @Parameter(description = "삭제할 멤버의 PUUID")
        @PathVariable puuid: String,
    ): CommonApiResponse<Unit> {
        deleteMemberUseCase.deleteMember(puuidOf(puuid))
        return CommonApiResponse.success(Unit)
    }
}
