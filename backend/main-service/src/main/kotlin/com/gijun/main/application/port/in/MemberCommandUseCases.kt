package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.command.RegisterMemberCommand
import com.gijun.main.application.dto.command.RegisterMembersCommand
import com.gijun.main.application.dto.result.MemberResult
import com.gijun.main.application.dto.result.RegisterMembersResult
import com.gijun.main.shared.domain.vo.Puuid

interface RegisterMemberUseCase {
    fun registerMember(command: RegisterMemberCommand): MemberResult
}

interface RegisterMembersUseCase {
    /** 여러 명을 한 번에. 한 건이 실패해도 나머지는 계속하고, 건별 결과를 돌려준다. */
    fun registerMembers(command: RegisterMembersCommand): RegisterMembersResult
}

interface DeleteMemberUseCase {
    fun deleteMember(puuid: Puuid)
}
