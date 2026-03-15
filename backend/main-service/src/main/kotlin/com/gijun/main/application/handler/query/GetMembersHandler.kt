package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.MemberDto
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.out.MemberPort
import com.gijun.main.domain.model.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMembersHandler(private val memberPort: MemberPort) : GetMembersUseCase {
    override fun getAll(): List<MemberDto> = memberPort.findAll().map { it.toDto() }
    private fun Member.toDto() = MemberDto(riotId, puuid, registeredAt)
}
