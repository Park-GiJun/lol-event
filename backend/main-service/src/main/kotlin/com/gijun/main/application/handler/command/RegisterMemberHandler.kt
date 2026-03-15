package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.*
import com.gijun.main.application.port.`in`.DeleteMemberUseCase
import com.gijun.main.application.port.`in`.RegisterMemberUseCase
import com.gijun.main.application.port.out.MemberPort
import com.gijun.main.application.port.out.RiotApiPort
import com.gijun.main.domain.model.Member
import com.gijun.common.exception.DomainAlreadyExistsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RegisterMemberHandler(
    private val memberPort: MemberPort,
    private val riotApiPort: RiotApiPort
) : RegisterMemberUseCase, DeleteMemberUseCase {

    override fun register(request: RegisterMemberRequest): MemberDto {
        val (gameName, tagLine) = parseRiotId(request.riotId)
        val account = riotApiPort.getAccount(gameName, tagLine)
        if (memberPort.existsByPuuid(account.puuid))
            throw DomainAlreadyExistsException("이미 등록된 멤버입니다: ${request.riotId}")
        val member = memberPort.save(Member(riotId = "${account.gameName}#${account.tagLine}", puuid = account.puuid))
        return member.toDto()
    }

    override fun registerBulk(request: RegisterBulkRequest): BulkRegisterResponse {
        val results = mutableListOf<BulkRegisterResult>()
        for (raw in request.riotIds) {
            val riotId = raw.trim()
            if (!riotId.contains('#')) {
                results.add(BulkRegisterResult(riotId, "skip", "형식 오류")); continue
            }
            val (gameName, tagLine) = parseRiotId(riotId)
            try {
                val account = riotApiPort.getAccount(gameName, tagLine)
                if (memberPort.existsByPuuid(account.puuid)) {
                    results.add(BulkRegisterResult(riotId, "skip", "중복")); continue
                }
                memberPort.save(Member(riotId = "${account.gameName}#${account.tagLine}", puuid = account.puuid))
                results.add(BulkRegisterResult("${account.gameName}#${account.tagLine}", "ok"))
            } catch (e: Exception) {
                results.add(BulkRegisterResult(riotId, "error", e.message))
            }
        }
        return BulkRegisterResponse(results, memberPort.findAll().size)
    }

    override fun delete(puuid: String) = memberPort.deleteByPuuid(puuid)

    private fun parseRiotId(riotId: String): Pair<String, String> {
        val parts = riotId.split('#', limit = 2)
        return Pair(parts[0], parts.getOrElse(1) { "" })
    }

    private fun Member.toDto() = MemberDto(riotId, puuid, registeredAt)
}
