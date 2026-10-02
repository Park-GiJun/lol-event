package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.MemberResult

interface GetMembersUseCase {
    fun getMembers(): List<MemberResult>
}
