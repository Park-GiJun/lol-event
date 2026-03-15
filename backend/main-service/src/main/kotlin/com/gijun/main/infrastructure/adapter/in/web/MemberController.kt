package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.member.command.RegisterBulkCommand
import com.gijun.main.application.dto.member.command.RegisterMemberCommand
import com.gijun.main.application.port.`in`.DeleteMemberUseCase
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.`in`.RegisterMemberUseCase
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
    fun register(@RequestBody command: RegisterMemberCommand) =
        CommonApiResponse.created(registerMemberUseCase.register(command))

    @PostMapping("/register-bulk")
    fun registerBulk(@RequestBody command: RegisterBulkCommand) =
        CommonApiResponse.success(registerMemberUseCase.registerBulk(command))

    @DeleteMapping("/{puuid}")
    fun delete(@PathVariable puuid: String): CommonApiResponse<Unit> {
        deleteMemberUseCase.delete(puuid)
        return CommonApiResponse.success(Unit)
    }
}
