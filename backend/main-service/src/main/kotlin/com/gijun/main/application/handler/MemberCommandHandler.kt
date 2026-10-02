package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.RegisterMemberCommand
import com.gijun.main.application.dto.command.RegisterMembersCommand
import com.gijun.main.application.dto.result.MemberResult
import com.gijun.main.application.dto.result.RegisterMembersItemResult
import com.gijun.main.application.dto.result.RegisterMembersResult
import com.gijun.main.application.port.`in`.DeleteMemberUseCase
import com.gijun.main.application.port.`in`.RegisterMemberUseCase
import com.gijun.main.application.port.`in`.RegisterMembersUseCase
import com.gijun.main.application.port.out.external.RiotApiKtorPort
import com.gijun.main.application.port.out.persistence.MemberCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.domain.member.exception.MemberAlreadyExistsException
import com.gijun.main.domain.member.model.MemberModel
import com.gijun.main.shared.domain.vo.Puuid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 내전 멤버 등록·삭제.
 */
@Service
@Transactional
class MemberCommandHandler(
    private val memberQueryPersistencePort: MemberQueryPersistencePort,
    private val memberCommandPersistencePort: MemberCommandPersistencePort,
    private val riotApiKtorPort: RiotApiKtorPort,
) : RegisterMemberUseCase,
    RegisterMembersUseCase,
    DeleteMemberUseCase {
    override fun registerMember(command: RegisterMemberCommand): MemberResult {
        val (gameName, tagLine) = parseRiotId(command.riotId)
        val account = riotApiKtorPort.getAccount(gameName, tagLine)
        if (memberQueryPersistencePort.existsByPuuid(Puuid(account.puuid))) {
            throw MemberAlreadyExistsException(command.riotId)
        }
        val member =
            memberCommandPersistencePort.save(
                MemberModel(riotId = "${account.gameName}#${account.tagLine}", puuid = account.puuid),
            )
        return MemberResult.from(member)
    }

    override fun registerMembers(command: RegisterMembersCommand): RegisterMembersResult {
        val results = mutableListOf<RegisterMembersItemResult>()
        for (raw in command.riotIds) {
            val riotId = raw.trim()
            if (!riotId.contains('#')) {
                results.add(RegisterMembersItemResult(riotId, "skip", "형식 오류"))
                continue
            }
            val (gameName, tagLine) = parseRiotId(riotId)
            try {
                val account = riotApiKtorPort.getAccount(gameName, tagLine)
                if (memberQueryPersistencePort.existsByPuuid(Puuid(account.puuid))) {
                    results.add(RegisterMembersItemResult(riotId, "skip", "중복"))
                    continue
                }
                memberCommandPersistencePort.save(MemberModel(riotId = "${account.gameName}#${account.tagLine}", puuid = account.puuid))
                results.add(RegisterMembersItemResult("${account.gameName}#${account.tagLine}", "ok"))
            } catch (e: Exception) {
                results.add(RegisterMembersItemResult(riotId, "error", e.message))
            }
        }
        return RegisterMembersResult(results, memberQueryPersistencePort.findAll().size)
    }

    override fun deleteMember(puuid: Puuid) = memberCommandPersistencePort.deleteByPuuid(puuid)

    private fun parseRiotId(riotId: String): Pair<String, String> {
        val parts = riotId.split('#', limit = 2)
        return Pair(parts[0], parts.getOrElse(1) { "" })
    }
}
