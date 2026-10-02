package com.gijun.main.infrastructure.adapter.`in`.web.match

import com.gijun.main.application.dto.command.SaveMatchesCommand
import com.gijun.main.application.dto.result.SaveMatchesResult
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.GetMatchPageUseCase
import com.gijun.main.application.port.`in`.GetMatchTimelineUseCase
import com.gijun.main.application.port.`in`.GetMatchUseCase
import com.gijun.main.application.port.`in`.GetMatchesUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import com.gijun.main.shared.domain.vo.MatchId
import com.gijun.main.shared.infrastructure.web.common.WebMvcTestConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * `/api/matches/bulk` 는 **이미 배포된 데스크탑 수집기가 쓰는 계약**이다. 본문을 요청 DTO 로
 * 받도록 바꿨으므로, 수집기가 보내는 모양이 그대로 커맨드까지 닿는지 여기서 못박는다.
 */
@WebMvcTest(MatchWebAdapter::class)
@Import(WebMvcTestConfig::class)
class MatchWebAdapterTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getMatchesUseCase: GetMatchesUseCase

    @MockitoBean
    private lateinit var getMatchPageUseCase: GetMatchPageUseCase

    @MockitoBean
    private lateinit var getMatchUseCase: GetMatchUseCase

    @MockitoBean
    private lateinit var getMatchTimelineUseCase: GetMatchTimelineUseCase

    @MockitoBean
    private lateinit var saveMatchesUseCase: SaveMatchesUseCase

    @MockitoBean
    private lateinit var deleteMatchUseCase: DeleteMatchUseCase

    @Test
    fun `수집기가 올린 본문이 커맨드로 옮겨진다`() {
        whenever(saveMatchesUseCase.saveMatches(any())).thenReturn(SaveMatchesResult(saved = 1, skipped = 0, total = 42))

        mockMvc
            .post("/api/matches/bulk") {
                contentType = MediaType.APPLICATION_JSON
                content = BULK_BODY
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.saved") { value(1) }
                jsonPath("$.data.skipped") { value(0) }
                jsonPath("$.data.total") { value(42) }
            }

        val captor = argumentCaptor<SaveMatchesCommand>()
        verify(saveMatchesUseCase).saveMatches(captor.capture())
        val match = captor.firstValue.matches.single()
        assertEquals("KR_8126722699", match.matchId)
        assertEquals(3130, match.queueId)
        assertEquals(1_757_000_000_000, match.gameCreation)
        assertEquals("{\"frames\":[]}", match.timelineRaw)

        val participant = match.participants.single()
        assertEquals("Hide on bush#KR1", participant.riotId)
        assertEquals("puuid-1", participant.puuid)
        assertEquals("Ahri", participant.champion)
        assertEquals(7, participant.kills)
        // 본문에 없는 필드는 기본값으로 채워진다 — 옛 수집기는 필드를 덜 보낸다.
        assertEquals(0, participant.deaths)

        val team = match.teams.single()
        assertEquals(100, team.teamId)
        assertEquals(157, team.bans.single().championId)
    }

    @Test
    fun `참가자의 puuid 는 null 이어도 받는다`() {
        whenever(saveMatchesUseCase.saveMatches(any())).thenReturn(SaveMatchesResult(1, 0, 1))

        mockMvc
            .post("/api/matches/bulk") {
                contentType = MediaType.APPLICATION_JSON
                content = BULK_BODY.replace("\"puuid-1\"", "null")
            }.andExpect { status { isOk() } }

        val captor = argumentCaptor<SaveMatchesCommand>()
        verify(saveMatchesUseCase).saveMatches(captor.capture())
        assertNull(
            captor.firstValue.matches
                .single()
                .participants
                .single()
                .puuid,
        )
    }

    @Test
    fun `서버가 모르는 필드가 섞여 와도 받는다`() {
        // 수집기가 서버보다 먼저 올라가면 새 필드를 보낸다. 그걸로 수집이 막히면 안 된다.
        whenever(saveMatchesUseCase.saveMatches(any())).thenReturn(SaveMatchesResult(1, 0, 1))

        mockMvc
            .post("/api/matches/bulk") {
                contentType = MediaType.APPLICATION_JSON
                content = BULK_BODY.replace("\"queueId\": 3130,", "\"queueId\": 3130, \"fieldFromTheFuture\": true,")
            }.andExpect { status { isOk() } }
    }

    @Test
    fun `matchId 가 비면 400 이고 유즈케이스를 부르지 않는다`() {
        mockMvc
            .post("/api/matches/bulk") {
                contentType = MediaType.APPLICATION_JSON
                content = BULK_BODY.replace("KR_8126722699", " ")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_FAILED") }
            }

        verifyNoInteractions(saveMatchesUseCase)
    }

    @Test
    fun `없는 경기는 404 다`() {
        mockMvc
            .get("/api/matches/KR_404")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("NOT_FOUND") }
            }
    }

    @Test
    fun `타임라인도 경기가 없을 때만 404 다`() {
        mockMvc.get("/api/matches/KR_404/timeline").andExpect { status { isNotFound() } }
    }

    @Test
    fun `삭제는 경로의 matchId 를 그대로 넘긴다`() {
        mockMvc.delete("/api/matches/KR_1").andExpect { status { isOk() } }

        verify(deleteMatchUseCase).deleteMatch(MatchId("KR_1"))
    }

    private companion object {
        private val BULK_BODY =
            """
            {
              "matches": [
                {
                  "matchId": "KR_8126722699",
                  "queueId": 3130,
                  "gameCreation": 1757000000000,
                  "gameDuration": 1800,
                  "timelineRaw": "{\"frames\":[]}",
                  "participants": [
                    {
                      "participantId": 1,
                      "puuid": "puuid-1",
                      "riotId": "Hide on bush#KR1",
                      "champion": "Ahri",
                      "championId": 103,
                      "team": "BLUE",
                      "teamId": 100,
                      "win": true,
                      "kills": 7
                    }
                  ],
                  "teams": [
                    { "teamId": 100, "win": true, "bans": [ { "championId": 157, "championName": "Yasuo", "pickTurn": 1 } ] }
                  ]
                }
              ]
            }
            """.trimIndent()
    }
}
