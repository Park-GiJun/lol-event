package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.member.command.RegisterBulkCommand
import com.gijun.main.application.dto.member.command.RegisterMemberCommand
import com.gijun.main.application.dto.member.result.BulkRegisterResult
import com.gijun.main.application.dto.member.result.MemberResult

interface RegisterMemberUseCase {
    fun register(command: RegisterMemberCommand): MemberResult
    fun registerBulk(command: RegisterBulkCommand): BulkRegisterResult
}

interface GetMembersUseCase {
    fun getAll(): List<MemberResult>
}

interface DeleteMemberUseCase {
    fun delete(puuid: String)
}
