package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.dto.query.GetDuoStatsQuery
import com.gijun.main.application.dto.query.GetRivalMatchupQuery
import com.gijun.main.application.dto.result.DuoStat
import com.gijun.main.application.dto.result.DuoStatsResult
import com.gijun.main.application.dto.result.RivalMatchupResult
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.`in`.GetRivalMatchupUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.infrastructure.web.common.WebMvcTestConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * 통계 어댑터들이 공통으로 쓰는 `mode` 파라미터의 계약.
 *
 * 화면과 이미 배포된 수집기는 소문자 키(`normal` · `all`)를 보낸다. enum 으로 바꾸면서
 * Spring 기본 변환(상수 이름 `NORMAL` 만 통과)에 맡기면 전 요청이 400 이 된다.
 */
@WebMvcTest(TeamStatsWebAdapter::class)
@Import(WebMvcTestConfig::class)
class TeamStatsWebAdapterTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getDuoStatsUseCase: GetDuoStatsUseCase

    @MockitoBean
    private lateinit var getRivalMatchupUseCase: GetRivalMatchupUseCase

    private val duo =
        DuoStat(
            player1 = "가#KR1",
            player2 = "나#KR1",
            games = 10,
            wins = 6,
            winRate = 60,
            adjustedWinRate = 55.5,
            sampleGrade = "MEDIUM",
            avgKills = 12.3,
            avgDeaths = 8.1,
            avgAssists = 20.4,
            kda = 4.03,
        )

    @Test
    fun `소문자 mode 와 minGames 가 Query 로 조립돼 넘어간다`() {
        whenever(getDuoStatsUseCase.getDuoStats(any())).thenReturn(DuoStatsResult(listOf(duo)))

        mockMvc
            .get("/api/stats/duo") {
                param("mode", "all")
                param("minGames", "2")
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.duos[0].player1") { value("가#KR1") }
                jsonPath("$.data.duos[0].winRate") { value(60) }
                jsonPath("$.data.duos[0].adjustedWinRate") { value(55.5) }
                jsonPath("$.data.duos[0].sampleGrade") { value("MEDIUM") }
            }

        verify(getDuoStatsUseCase).getDuoStats(GetDuoStatsQuery(GameMode.ALL, 2))
    }

    @Test
    fun `mode 와 minGames 를 생략하면 normal 과 3 이다`() {
        whenever(getDuoStatsUseCase.getDuoStats(any())).thenReturn(DuoStatsResult(emptyList()))

        mockMvc.get("/api/stats/duo").andExpect { status { isOk() } }

        verify(getDuoStatsUseCase).getDuoStats(GetDuoStatsQuery(GameMode.NORMAL, 3))
    }

    @Test
    fun `mode 는 대소문자를 가리지 않는다`() {
        whenever(getRivalMatchupUseCase.getRivalMatchup(any())).thenReturn(RivalMatchupResult(emptyList(), null))

        mockMvc.get("/api/stats/rival-matchup") { param("mode", "ARAM") }.andExpect { status { isOk() } }

        verify(getRivalMatchupUseCase).getRivalMatchup(GetRivalMatchupQuery(GameMode.ARAM, 3))
    }

    @Test
    fun `모르는 mode 는 400 이다 — 예전처럼 normal 로 떨어지지 않는다`() {
        mockMvc
            .get("/api/stats/duo") { param("mode", "arm") }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_GAME_MODE") }
            }

        verifyNoInteractions(getDuoStatsUseCase)
    }

    @Test
    fun `숫자 자리에 문자가 오면 400 TYPE_MISMATCH 다`() {
        mockMvc
            .get("/api/stats/duo") { param("minGames", "many") }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("TYPE_MISMATCH") }
            }
    }
}
