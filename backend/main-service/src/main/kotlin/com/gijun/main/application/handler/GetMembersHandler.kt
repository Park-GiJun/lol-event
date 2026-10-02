package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.MemberResult
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMembersHandler(
    private val memberQueryPersistencePort: MemberQueryPersistencePort,
) : GetMembersUseCase {
    override fun getAll(): List<MemberResult> = memberQueryPersistencePort.findAll().map { MemberResult.from(it) }
}
