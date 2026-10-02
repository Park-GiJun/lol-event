package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.RegisterMemberCommand
import com.gijun.main.application.dto.command.RegisterMembersCommand
import com.gijun.main.application.port.out.external.RiotAccount
import com.gijun.main.application.port.out.external.RiotApiKtorPort
import com.gijun.main.application.port.out.persistence.MemberCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.domain.member.exception.MemberAlreadyExistsException
import com.gijun.main.domain.member.model.MemberModel
import com.gijun.main.domain.riot.exception.RiotAccountNotFoundException
import com.gijun.main.shared.domain.vo.Puuid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MemberCommandHandlerTest {
    private val memberQueryPersistencePort = mock<MemberQueryPersistencePort>()
    private val memberCommandPersistencePort =
        mock<MemberCommandPersistencePort> {
            on { save(any()) } doAnswer { it.getArgument<MemberModel>(0) }
        }
    private val riotApiKtorPort = mock<RiotApiKtorPort>()
    private val handler = MemberCommandHandler(memberQueryPersistencePort, memberCommandPersistencePort, riotApiKtorPort)

    private fun riotKnows(
        gameName: String,
        tagLine: String,
        puuid: String,
        canonical: String = gameName,
    ) {
        whenever(
            riotApiKtorPort.getAccount(gameName, tagLine),
        ).doReturn(RiotAccount(puuid = puuid, gameName = canonical, tagLine = tagLine))
    }

    private fun alreadyMember(puuid: String) {
        whenever(memberQueryPersistencePort.existsByPuuid(Puuid(puuid))).doReturn(true)
    }

    @Test
    fun `등록 - Riot 이 돌려준 표기로 저장한다`() {
        // 사용자가 소문자로 쳐도 Riot 계정의 정식 표기가 정본이다.
        riotKnows("hide on bush", "KR1", puuid = "p-1", canonical = "Hide on bush")

        val result = handler.registerMember(RegisterMemberCommand("hide on bush#KR1"))

        assertEquals("Hide on bush#KR1", result.riotId)
        assertEquals("p-1", result.puuid)
        val captor = argumentCaptor<MemberModel>()
        verify(memberCommandPersistencePort).save(captor.capture())
        assertEquals("Hide on bush#KR1", captor.firstValue.riotId)
    }

    @Test
    fun `등록 - 이미 있는 PUUID 면 충돌이고 저장하지 않는다`() {
        riotKnows("Faker", "KR1", puuid = "p-1")
        alreadyMember("p-1")

        assertThrows(MemberAlreadyExistsException::class.java) { handler.registerMember(RegisterMemberCommand("Faker#KR1")) }
        verify(memberCommandPersistencePort, never()).save(any())
    }

    @Test
    fun `일괄 등록 - 한 건이 실패해도 나머지는 계속하고 건별 결과를 남긴다`() {
        riotKnows("신규", "KR1", puuid = "p-new")
        riotKnows("중복", "KR1", puuid = "p-dup")
        alreadyMember("p-dup")
        whenever(riotApiKtorPort.getAccount("없는사람", "KR1")).doThrow(RiotAccountNotFoundException("없는사람#KR1"))
        whenever(
            memberQueryPersistencePort.findAll(),
        ).doReturn(listOf(MemberModel(riotId = "신규#KR1", puuid = "p-new"), MemberModel(riotId = "중복#KR1", puuid = "p-dup")))

        val result =
            handler.registerMembers(
                RegisterMembersCommand(listOf("태그없음", " 신규#KR1 ", "중복#KR1", "없는사람#KR1")),
            )

        assertEquals(listOf("skip", "ok", "skip", "error"), result.results.map { it.status })
        assertEquals(listOf("형식 오류", null, "중복"), result.results.take(3).map { it.reason })
        assertEquals(2, result.total)
        verify(memberCommandPersistencePort).save(any())
    }

    @Test
    fun `삭제 - PUUID 로 지운다`() {
        handler.deleteMember(Puuid("p-1"))

        verify(memberCommandPersistencePort).deleteByPuuid(Puuid("p-1"))
    }
}
