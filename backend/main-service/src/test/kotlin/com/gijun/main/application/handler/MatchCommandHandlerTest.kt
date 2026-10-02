package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.IngestMatchCommand
import com.gijun.main.application.dto.command.MatchInput
import com.gijun.main.application.dto.command.ParticipantInput
import com.gijun.main.application.dto.command.SaveMatchesCommand
import com.gijun.main.application.dto.result.SaveMatchesResult
import com.gijun.main.application.port.out.cache.StatsResultCacheCommandPort
import com.gijun.main.application.port.out.messaging.MatchEventPublishPort
import com.gijun.main.application.port.out.persistence.MatchCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.MemberCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.domain.match.enums.LaneMethod
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.member.model.MemberModel
import com.gijun.main.shared.domain.vo.MatchId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class MatchCommandHandlerTest {
    private val matchQueryPersistencePort = mock<MatchQueryPersistencePort>()
    private val matchCommandPersistencePort =
        mock<MatchCommandPersistencePort> {
            on { save(any()) } doAnswer { it.getArgument<MatchModel>(0) }
        }
    private val memberQueryPersistencePort = mock<MemberQueryPersistencePort>()
    private val memberCommandPersistencePort = mock<MemberCommandPersistencePort>()
    private val statsResultCacheCommandPort = mock<StatsResultCacheCommandPort>()
    private val matchEventPublishPort = mock<MatchEventPublishPort>()

    private val handler =
        MatchCommandHandler(
            matchQueryPersistencePort,
            matchCommandPersistencePort,
            memberQueryPersistencePort,
            memberCommandPersistencePort,
            statsResultCacheCommandPort,
            matchEventPublishPort,
        )

    private fun participant(
        riotId: String,
        puuid: String?,
        teamId: Int = 100,
    ) = ParticipantInput(puuid = puuid, riotId = riotId, champion = "Ahri", team = "BLUE", teamId = teamId, win = teamId == 100)

    private fun input(
        matchId: String,
        timelineRaw: String? = null,
        participants: List<ParticipantInput> = listOf(participant("가#KR1", "p-1")),
    ) = MatchInput(
        matchId = matchId,
        queueId = 3130,
        gameCreation = 1_757_000_000_000,
        gameDuration = 1800,
        timelineRaw = timelineRaw,
        participants = participants,
    )

    // ────────── saveMatches ──────────

    @Test
    fun `저장 - 이미 있는 경기는 건너뛰고 새 경기만 저장하고 알린다`() {
        whenever(matchQueryPersistencePort.existsByMatchId(MatchId("KR_OLD"))).doReturn(true)
        whenever(matchQueryPersistencePort.countByQueueIds(any())).doReturn(11L)

        val result = handler.saveMatches(SaveMatchesCommand(listOf(input("KR_OLD"), input("KR_NEW"))))

        assertEquals(SaveMatchesResult(saved = 1, skipped = 1, total = 11), result)
        val captor = argumentCaptor<MatchModel>()
        verify(matchCommandPersistencePort).save(captor.capture())
        assertEquals("KR_NEW", captor.firstValue.matchId)
        verify(matchEventPublishPort).publishRatingRequested(MatchId("KR_NEW"))
        verify(matchEventPublishPort).publishStatsRebuildRequested(MatchId("KR_NEW"))
        verify(matchEventPublishPort, never()).publishRatingRequested(MatchId("KR_OLD"))
        verify(statsResultCacheCommandPort).evictAll()
    }

    @Test
    fun `저장 - 새 경기가 없으면 캐시를 비우지 않고 아무것도 알리지 않는다`() {
        whenever(matchQueryPersistencePort.existsByMatchId(MatchId("KR_1"))).doReturn(true)
        whenever(matchQueryPersistencePort.existsByMatchId(MatchId("KR_2"))).doReturn(true)

        val result = handler.saveMatches(SaveMatchesCommand(listOf(input("KR_1"), input("KR_2"))))

        assertEquals(0, result.saved)
        assertEquals(2, result.skipped)
        verify(statsResultCacheCommandPort, never()).evictAll()
        verifyNoInteractions(matchEventPublishPort)
    }

    @Test
    fun `저장 - 캐시는 경기 수와 무관하게 한 번만 비운다`() {
        handler.saveMatches(SaveMatchesCommand(listOf(input("KR_1"), input("KR_2"), input("KR_3"))))

        verify(statsResultCacheCommandPort, times(1)).evictAll()
        listOf("KR_1", "KR_2", "KR_3").forEach { verify(matchEventPublishPort).publishRatingRequested(MatchId(it)) }
    }

    @Test
    fun `저장 - 타임라인은 있을 때만 따로 저장한다`() {
        handler.saveMatches(SaveMatchesCommand(listOf(input("KR_WITH", timelineRaw = "{\"frames\":[]}"), input("KR_WITHOUT"))))

        verify(matchCommandPersistencePort).saveTimelineRaw(MatchId("KR_WITH"), "{\"frames\":[]}")
        val timelineSaves = mockingDetails(matchCommandPersistencePort).invocations.count { it.method.name.startsWith("saveTimelineRaw") }
        assertEquals(1, timelineSaves, "타임라인이 없는 경기에는 저장을 부르지 않는다")
    }

    @Test
    fun `저장 - 라인 판정 방법을 확정해 같이 저장한다`() {
        handler.saveMatches(SaveMatchesCommand(listOf(input("KR_1"))))

        val captor = argumentCaptor<MatchModel>()
        verify(matchCommandPersistencePort).save(captor.capture())
        // 타임라인이 없으면 최종 스탯 기준이다.
        assertEquals(LaneMethod.LEGACY_FINAL, captor.firstValue.laneMethod)
    }

    // ────────── ingestMatch ──────────

    @Test
    fun `수집 - 이미 있는지 묻지 않고 저장하고 알린다`() {
        handler.ingestMatch(IngestMatchCommand(input("KR_1")))

        verifyNoInteractions(matchQueryPersistencePort)
        verify(matchCommandPersistencePort).save(any())
        verify(matchEventPublishPort).publishRatingRequested(MatchId("KR_1"))
        verify(matchEventPublishPort).publishStatsRebuildRequested(MatchId("KR_1"))
    }

    @Test
    fun `수집 - 아직 멤버가 아닌 참가자만 등록한다`() {
        whenever(memberQueryPersistencePort.findAllPuuidsByPuuidIn(any())).doReturn(listOf("p-old"))

        handler.ingestMatch(
            IngestMatchCommand(
                input(
                    "KR_1",
                    participants =
                        listOf(
                            participant("기존#KR1", "p-old"),
                            participant("신규#KR1", "p-new"),
                            // 봇처럼 PUUID 가 없는 참가자는 멤버가 될 수 없다.
                            participant("봇", null, teamId = 200),
                        ),
                ),
            ),
        )

        val captor = argumentCaptor<List<MemberModel>>()
        verify(memberCommandPersistencePort).saveAll(captor.capture())
        assertEquals(listOf("신규#KR1" to "p-new"), captor.firstValue.map { it.riotId to it.puuid })
    }

    @Test
    fun `수집 - 새로 등록할 사람이 없으면 저장을 부르지 않는다`() {
        whenever(memberQueryPersistencePort.findAllPuuidsByPuuidIn(any())).doReturn(listOf("p-1"))

        handler.ingestMatch(IngestMatchCommand(input("KR_1")))

        verify(memberCommandPersistencePort, never()).saveAll(any())
    }

    // ────────── deleteMatch ──────────

    @Test
    fun `삭제 - 지우고 통계 캐시를 비운다`() {
        handler.deleteMatch(MatchId("KR_1"))

        verify(matchCommandPersistencePort).deleteByMatchId(MatchId("KR_1"))
        verify(statsResultCacheCommandPort).evictAll()
    }
}
