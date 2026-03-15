package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.*

interface RegisterMemberUseCase {
    fun register(request: RegisterMemberRequest): MemberDto
    fun registerBulk(request: RegisterBulkRequest): BulkRegisterResponse
}

interface GetMembersUseCase {
    fun getAll(): List<MemberDto>
}

interface DeleteMemberUseCase {
    fun delete(puuid: String)
}
