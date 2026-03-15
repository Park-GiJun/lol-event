package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.member.result.MemberResult
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.out.MemberPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMembersHandler(private val memberPersistencePort: MemberPersistencePort) : GetMembersUseCase {
    override fun getAll(): List<MemberResult> = memberPersistencePort.findAll().map { MemberResult.from(it) }
}
