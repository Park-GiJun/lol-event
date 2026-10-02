package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.RiotMasteryResult
import com.gijun.main.application.dto.result.RiotProfileResult
import com.gijun.main.application.dto.result.RiotRankResult
import com.gijun.main.application.port.out.external.ChampionMasteryData
import com.gijun.main.application.port.out.external.RankedEntry
import com.gijun.main.application.port.out.external.RiotApiKtorPort
import com.gijun.main.application.port.out.external.SummonerData
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.domain.member.model.MemberModel
import com.gijun.main.shared.domain.vo.RiotId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class RiotProfileQueryHandlerTest {
    private val riotApiKtorPort = mock<RiotApiKtorPort>()
    private val memberQueryPersistencePort = mock<MemberQueryPersistencePort>()
    private val handler = RiotProfileQueryHandler(riotApiKtorPort, memberQueryPersistencePort)

    private val me = RiotId("나#KR1")

    private fun registered(vararg riotIds: String) {
        whenever(memberQueryPersistencePort.findAll()).doReturn(riotIds.map { MemberModel(riotId = it, puuid = "puuid-$it") })
    }

    private val summoner = SummonerData(id = "s-1", accountId = "a-1", puuid = "puuid-나#KR1", profileIconId = 29, summonerLevel = 311)

    @Test
    fun `등록되지 않은 사람이면 Riot 을 부르지 않고 빈 프로필을 준다`() {
        registered("다른사람#KR1")

        val result = handler.getRiotProfile(me)

        assertEquals(RiotProfileResult.unregistered("나#KR1"), result)
        verifyNoInteractions(riotApiKtorPort)
    }

    @Test
    fun `랭크와 숙련도를 채운다`() {
        registered("나#KR1")
        whenever(riotApiKtorPort.getSummonerByPuuid("puuid-나#KR1")).doReturn(summoner)
        whenever(riotApiKtorPort.getRankedEntries("s-1")).doReturn(
            listOf(
                RankedEntry("RANKED_SOLO_5x5", "GOLD", "II", leaguePoints = 40, wins = 30, losses = 10),
                RankedEntry("RANKED_TFT", "IRON", "IV", leaguePoints = 0, wins = 0, losses = 0),
            ),
        )
        whenever(riotApiKtorPort.getChampionMastery("puuid-나#KR1", 10)).doReturn(listOf(ChampionMasteryData(103, 7, 123_456)))

        val result = handler.getRiotProfile(me)

        assertEquals(311L, result.summonerLevel)
        assertEquals(RiotRankResult("GOLD", "II", lp = 40, wins = 30, losses = 10, winRate = 75.0), result.soloRank)
        assertNull(result.flexRank, "자유 랭크 기록이 없다")
        assertEquals(listOf(RiotMasteryResult(championId = 103, level = 7, points = 123_456)), result.topMastery)
    }

    @Test
    fun `소환사 조회가 실패해도 숙련도는 버리지 않는다`() {
        registered("나#KR1")
        whenever(riotApiKtorPort.getSummonerByPuuid(any())).doThrow(IllegalStateException("Riot 5xx"))
        whenever(riotApiKtorPort.getChampionMastery(any(), any())).doReturn(listOf(ChampionMasteryData(103, 7, 1)))

        val result = handler.getRiotProfile(me)

        assertNull(result.summonerLevel)
        assertNull(result.soloRank)
        assertEquals(1, result.topMastery.size)
    }

    @Test
    fun `판수가 없으면 승률은 0 이다`() {
        registered("나#KR1")
        whenever(riotApiKtorPort.getSummonerByPuuid(any())).doReturn(summoner)
        whenever(riotApiKtorPort.getRankedEntries(any())).doReturn(listOf(RankedEntry("RANKED_FLEX_SR", "IRON", "IV", 0, 0, 0)))

        assertEquals(0.0, handler.getRiotProfile(me).flexRank?.winRate)
    }

    @Test
    fun `일괄 조회 - 앞에서 10 명까지만 본다`() {
        val ids = (1..13).map { "p$it#KR1" }
        registered(*ids.toTypedArray())

        val result = handler.getRiotProfiles(ids)

        assertEquals(ids.take(10), result.keys.toList())
        // 사람당 한 번씩만 멤버 목록을 읽는다.
        verify(memberQueryPersistencePort, times(10)).findAll()
    }

    @Test
    fun `일괄 조회 - 빈 문자열처럼 Riot ID 가 될 수 없는 값은 건너뛴다`() {
        registered("나#KR1")

        val result = handler.getRiotProfiles(listOf(" ", "나#KR1"))

        assertEquals(listOf("나#KR1"), result.keys.toList())
    }
}
