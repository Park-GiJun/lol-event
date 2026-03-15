package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.member.command.RegisterBulkCommand
import com.gijun.main.application.dto.member.command.RegisterMemberCommand
import com.gijun.main.application.dto.member.result.BulkRegisterItemResult
import com.gijun.main.application.dto.member.result.BulkRegisterResult
import com.gijun.main.application.dto.member.result.MemberResult
import com.gijun.main.application.port.`in`.DeleteMemberUseCase
import com.gijun.main.application.port.`in`.RegisterMemberUseCase
import com.gijun.main.application.port.out.MemberPersistencePort
import com.gijun.main.application.port.out.RiotApiPort
import com.gijun.main.domain.model.member.Member
import com.gijun.common.exception.DomainAlreadyExistsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RegisterMemberHandler(
    private val memberPersistencePort: MemberPersistencePort,
    private val riotApiPort: RiotApiPort
) : RegisterMemberUseCase, DeleteMemberUseCase {

    override fun register(command: RegisterMemberCommand): MemberResult {
        val (gameName, tagLine) = parseRiotId(command.riotId)
        val account = riotApiPort.getAccount(gameName, tagLine)
        if (memberPersistencePort.existsByPuuid(account.puuid))
            throw DomainAlreadyExistsException("이미 등록된 멤버입니다: ${command.riotId}")
        val member = memberPersistencePort.save(Member(riotId = "${account.gameName}#${account.tagLine}", puuid = account.puuid))
        return MemberResult.from(member)
    }

    override fun registerBulk(command: RegisterBulkCommand): BulkRegisterResult {
        val results = mutableListOf<BulkRegisterItemResult>()
        for (raw in command.riotIds) {
            val riotId = raw.trim()
            if (!riotId.contains('#')) {
                results.add(BulkRegisterItemResult(riotId, "skip", "형식 오류")); continue
            }
            val (gameName, tagLine) = parseRiotId(riotId)
            try {
                val account = riotApiPort.getAccount(gameName, tagLine)
                if (memberPersistencePort.existsByPuuid(account.puuid)) {
                    results.add(BulkRegisterItemResult(riotId, "skip", "중복")); continue
                }
                memberPersistencePort.save(Member(riotId = "${account.gameName}#${account.tagLine}", puuid = account.puuid))
                results.add(BulkRegisterItemResult("${account.gameName}#${account.tagLine}", "ok"))
            } catch (e: Exception) {
                results.add(BulkRegisterItemResult(riotId, "error", e.message))
            }
        }
        return BulkRegisterResult(results, memberPersistencePort.findAll().size)
    }

    override fun delete(puuid: String) = memberPersistencePort.deleteByPuuid(puuid)

    private fun parseRiotId(riotId: String): Pair<String, String> {
        val parts = riotId.split('#', limit = 2)
        return Pair(parts[0], parts.getOrElse(1) { "" })
    }
}
