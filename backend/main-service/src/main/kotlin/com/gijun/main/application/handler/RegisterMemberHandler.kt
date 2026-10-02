package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.RegisterBulkCommand
import com.gijun.main.application.dto.command.RegisterMemberCommand
import com.gijun.main.application.dto.result.BulkRegisterItemResult
import com.gijun.main.application.dto.result.BulkRegisterResult
import com.gijun.main.application.dto.result.MemberResult
import com.gijun.main.application.port.`in`.DeleteMemberUseCase
import com.gijun.main.application.port.`in`.RegisterMemberUseCase
import com.gijun.main.application.port.out.external.RiotApiKtorPort
import com.gijun.main.application.port.out.persistence.MemberCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.domain.member.exception.MemberAlreadyExistsException
import com.gijun.main.domain.member.model.MemberModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RegisterMemberHandler(
    private val memberQueryPersistencePort: MemberQueryPersistencePort,
    private val memberCommandPersistencePort: MemberCommandPersistencePort,
    private val riotApiKtorPort: RiotApiKtorPort,
) : RegisterMemberUseCase,
    DeleteMemberUseCase {
    override fun register(command: RegisterMemberCommand): MemberResult {
        val (gameName, tagLine) = parseRiotId(command.riotId)
        val account = riotApiKtorPort.getAccount(gameName, tagLine)
        if (memberQueryPersistencePort.existsByPuuid(account.puuid)) {
            throw MemberAlreadyExistsException(command.riotId)
        }
        val member =
            memberCommandPersistencePort.save(
                MemberModel(riotId = "${account.gameName}#${account.tagLine}", puuid = account.puuid),
            )
        return MemberResult.from(member)
    }

    override fun registerBulk(command: RegisterBulkCommand): BulkRegisterResult {
        val results = mutableListOf<BulkRegisterItemResult>()
        for (raw in command.riotIds) {
            val riotId = raw.trim()
            if (!riotId.contains('#')) {
                results.add(BulkRegisterItemResult(riotId, "skip", "형식 오류"))
                continue
            }
            val (gameName, tagLine) = parseRiotId(riotId)
            try {
                val account = riotApiKtorPort.getAccount(gameName, tagLine)
                if (memberQueryPersistencePort.existsByPuuid(account.puuid)) {
                    results.add(BulkRegisterItemResult(riotId, "skip", "중복"))
                    continue
                }
                memberCommandPersistencePort.save(MemberModel(riotId = "${account.gameName}#${account.tagLine}", puuid = account.puuid))
                results.add(BulkRegisterItemResult("${account.gameName}#${account.tagLine}", "ok"))
            } catch (e: Exception) {
                results.add(BulkRegisterItemResult(riotId, "error", e.message))
            }
        }
        return BulkRegisterResult(results, memberQueryPersistencePort.findAll().size)
    }

    override fun delete(puuid: String) = memberCommandPersistencePort.deleteByPuuid(puuid)

    private fun parseRiotId(riotId: String): Pair<String, String> {
        val parts = riotId.split('#', limit = 2)
        return Pair(parts[0], parts.getOrElse(1) { "" })
    }
}
