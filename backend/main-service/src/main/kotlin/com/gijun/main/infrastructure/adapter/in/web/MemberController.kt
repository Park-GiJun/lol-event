package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.*
import com.gijun.main.application.port.`in`.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/members")
class MemberController(
    private val registerMemberUseCase: RegisterMemberUseCase,
    private val getMembersUseCase: GetMembersUseCase,
    private val deleteMemberUseCase: DeleteMemberUseCase
) {
    @GetMapping
    fun getAll() = CommonApiResponse.success(getMembersUseCase.getAll())

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterMemberRequest) =
        CommonApiResponse.created(registerMemberUseCase.register(request))

    @PostMapping("/register-bulk")
    fun registerBulk(@RequestBody request: RegisterBulkRequest) =
        CommonApiResponse.success(registerMemberUseCase.registerBulk(request))

    @DeleteMapping("/{puuid}")
    fun delete(@PathVariable puuid: String): CommonApiResponse<Unit> {
        deleteMemberUseCase.delete(puuid)
        return CommonApiResponse.success(Unit)
    }
}
