package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.BuildTeamsCommand
import com.gijun.main.application.dto.command.BuildTeamsPlayer
import com.gijun.main.application.dto.result.MemberResult
import com.gijun.main.application.dto.result.PlayerRatingResult
import com.gijun.main.application.dto.result.TeamCandidatePositionResult
import com.gijun.main.application.dto.result.TeamCandidateResult
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.`in`.GetRatingsUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.external.LlmCompletionPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.PositionCount
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.rag.enums.RagDocumentType
import com.gijun.main.domain.rag.exception.RagUnavailableException
import com.gijun.main.domain.rating.service.RatingMath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import java.time.LocalDateTime

class TeamHandlersTest {
    private val lanes = listOf("TOP", "JUNGLE", "MID", "ADC", "SUPPORT")

    // ── 후보 ──────────────────────────────────────────────────────────────

    @Test
    fun `충분히 가 본 포지션만 기본으로 켠다`() {
        // 운영 데이터의 아랑택: 탑 53 · 미드 28 · 정글 18 · 원딜 14 · 서포터 0.
        val candidates =
            candidateHandler(
                members = listOf("아랑택#아랑택"),
                positionCounts =
                    listOf(
                        PositionCount("아랑택#아랑택", "TOP", 53),
                        PositionCount("아랑택#아랑택", "MID", 28),
                        PositionCount("아랑택#아랑택", "JUNGLE", 18),
                        PositionCount("아랑택#아랑택", "ADC", 14),
                    ),
            ).getTeamCandidates()

        val candidate = candidates.single()
        assertEquals(listOf("TOP", "JUNGLE", "MID", "ADC"), candidate.defaultPositions)
        assertEquals("TOP", candidate.mainPosition)
        assertEquals(113, candidate.games)
    }

    @Test
    fun `한두 판 간 자리는 갈 수 있는 자리로 치지 않는다`() {
        val candidate =
            candidateHandler(
                members = listOf("A#1"),
                positionCounts = listOf(PositionCount("A#1", "MID", 40), PositionCount("A#1", "SUPPORT", 2)),
            ).getTeamCandidates().single()

        assertEquals(listOf("MID"), candidate.defaultPositions)
        // 간 기록 자체는 남긴다. 화면이 판수를 보여 준다.
        assertEquals(listOf("MID", "SUPPORT"), candidate.positions.map { it.position })
    }

    @Test
    fun `기록이 없는 멤버는 시작 점수에 다섯 자리 전부다`() {
        val candidate = candidateHandler(members = listOf("신입#KR1"), positionCounts = emptyList()).getTeamCandidates().single()

        assertEquals(RatingMath.START, candidate.elo)
        assertEquals(lanes, candidate.defaultPositions)
        assertNull(candidate.mainPosition)
    }

    @Test
    fun `포지션을 못 정한 기록은 세지 않는다`() {
        val candidate =
            candidateHandler(
                members = listOf("A#1"),
                positionCounts = listOf(PositionCount("A#1", "UNKNOWN", 9), PositionCount("A#1", "", 4), PositionCount("A#1", "TOP", 5)),
            ).getTeamCandidates().single()

        assertEquals(5, candidate.games)
    }

    // ── 편성 ──────────────────────────────────────────────────────────────

    @Test
    fun `포지션을 안 주면 후보의 기본 포지션으로 짠다`() {
        val known = (1..9).map { candidate("p$it") } + candidate("탑만#1", defaults = listOf("TOP"))
        val result = buildHandler(known).buildTeams(command(known.map { it.riotId }, commentary = false))

        val slot = result.teams.flatMap { it.members }.first { it.riotId == "탑만#1" }
        assertEquals("TOP", slot.position)
        assertFalse(result.positionConflict)
    }

    @Test
    fun `이번 판에 고른 포지션이 기록보다 먼저다`() {
        val known = (1..9).map { candidate("p$it") } + candidate("탑만#1", defaults = listOf("TOP"))
        val players = known.map { BuildTeamsPlayer(it.riotId, if (it.riotId == "탑만#1") listOf(Position.SUPPORT) else null) }

        val result = buildHandler(known).buildTeams(BuildTeamsCommand(players, commentary = false))

        assertEquals(
            "SUPPORT",
            result.teams
                .flatMap { it.members }
                .first { it.riotId == "탑만#1" }
                .position,
        )
    }

    @Test
    fun `기록에 없는 손님도 시작 점수로 받는다`() {
        val known = (1..9).map { candidate("p$it") }
        val result = buildHandler(known).buildTeams(command(known.map { it.riotId } + "손님#KR1", commentary = false))

        assertEquals(
            RatingMath.START,
            result.teams
                .flatMap { it.members }
                .first { it.riotId == "손님#KR1" }
                .elo,
        )
    }

    @Test
    fun `두 팀이면 기대 승률의 합이 1 이다`() {
        val known = (1..10).map { candidate("p$it", elo = 1300.0 + it * 30) }
        val result = buildHandler(known).buildTeams(command(known.map { it.riotId }, commentary = false))

        assertEquals(1.0, result.teams.sumOf { it.winProbability }, 1e-9)
        assertEquals(listOf("1팀", "2팀"), result.teams.map { it.name })
    }

    @Test
    fun `해설을 요청하지 않으면 LLM 을 부르지 않는다`() {
        val llm = mock<LlmCompletionPort>()
        val known = (1..10).map { candidate("p$it") }

        val result = buildHandler(known, llm).buildTeams(command(known.map { it.riotId }, commentary = false))

        assertNull(result.commentary)
        assertNull(result.commentaryError)
        verifyNoInteractions(llm)
    }

    @Test
    fun `해설에는 확정된 편성과 묶음과 저장된 프로필이 사실로 들어간다`() {
        var facts = ""
        val llm =
            object : LlmCompletionPort {
                override fun complete(
                    system: String,
                    user: String,
                ): String {
                    facts = user
                    return "  1팀은 탑이 강합니다.  "
                }
            }
        val known = (1..10).map { candidate("p$it") }
        val stored = listOf("[플레이어] p", "전적: 10판 6승", "강점: KDA 5.00 — 탑 10명 중 1위.", "같은 팀으로 많이 한 사람: 생략").joinToString("\n")
        val profiles = mock<RagDocumentQueryPersistencePort> { on { findContent(any(), any()) } doReturn stored }

        val result =
            TeamBuildCommandHandler(candidates(known), profiles, llm)
                .buildTeams(BuildTeamsCommand(known.map { BuildTeamsPlayer(it.riotId) }, togetherGroups = listOf(listOf("p1", "p2"))))

        assertEquals("1팀은 탑이 강합니다.", result.commentary)
        assertTrue(facts.contains("1팀 — 평균 Elo")) { facts }
        assertTrue(facts.contains("같은 팀으로 묶음: p1, p2")) { facts }
        assertTrue(facts.contains("강점: KDA 5.00 — 탑 10명 중 1위.")) { facts }
        // 편성과 무관한 줄은 싣지 않는다. 30 명이면 컨텍스트를 다 쓴다.
        assertFalse(facts.contains("같은 팀으로 많이 한 사람")) { facts }
        assertTrue(facts.contains("## 라인별 맞대결")) { facts }
        assertTrue(facts.contains("가장 강한 라인")) { facts }
    }

    @Test
    fun `LLM 장비가 꺼져 있어도 편성은 돌려준다`() {
        val llm = mock<LlmCompletionPort> { on { complete(any(), any()) } doThrow RagUnavailableException() }
        val known = (1..10).map { candidate("p$it") }

        val result = buildHandler(known, llm).buildTeams(command(known.map { it.riotId }, commentary = true))

        assertEquals(2, result.teams.size)
        assertNull(result.commentary)
        assertNotNull(result.commentaryError)
    }

    private fun command(
        riotIds: List<String>,
        commentary: Boolean,
    ) = BuildTeamsCommand(riotIds.map { BuildTeamsPlayer(it) }, commentary = commentary)

    private fun candidate(
        riotId: String,
        elo: Double = 1500.0,
        defaults: List<String> = lanes,
    ) = TeamCandidateResult(riotId, elo, 10, defaults.first(), defaults.map { TeamCandidatePositionResult(it, 10) }, defaults)

    private fun candidates(known: List<TeamCandidateResult>) =
        object : GetTeamCandidatesUseCase {
            override fun getTeamCandidates() = known
        }

    private fun buildHandler(
        known: List<TeamCandidateResult>,
        llm: LlmCompletionPort = mock(),
    ) = TeamBuildCommandHandler(
        candidates(known),
        mock<RagDocumentQueryPersistencePort> { on { findContent(RagDocumentType.PLAYER_PROFILE, "x") } doReturn null },
        llm,
    )

    private fun candidateHandler(
        members: List<String>,
        positionCounts: List<PositionCount>,
    ) = TeamCandidateQueryHandler(
        getMembersUseCase =
            object : GetMembersUseCase {
                override fun getMembers() = members.map { MemberResult(it, "puuid-$it", LocalDateTime.MIN) }
            },
        getRatingsUseCase =
            object : GetRatingsUseCase {
                override fun getRatings(): List<PlayerRatingResult> = emptyList()
            },
        matchQueryPersistencePort = mock<MatchQueryPersistencePort> { on { findPositionCounts() } doReturn positionCounts },
        ratingHistoryQueryPersistencePort = mock(),
        statsResultCacheQueryPort =
            object : StatsResultCacheQueryPort {
                override fun <T> getOrCompute(
                    key: String,
                    compute: () -> T,
                ): T = compute()
            },
    )
}
