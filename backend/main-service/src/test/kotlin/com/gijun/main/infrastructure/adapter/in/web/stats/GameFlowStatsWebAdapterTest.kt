package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.dto.result.ComebackIndexEntry
import com.gijun.main.application.dto.result.ComebackIndexResult
import com.gijun.main.application.port.`in`.GetComebackIndexUseCase
import com.gijun.main.application.port.`in`.GetEarlyGameDominanceUseCase
import com.gijun.main.application.port.`in`.GetGameLengthTendencyUseCase
import com.gijun.main.application.port.`in`.GetObjectiveCorrelationUseCase
import com.gijun.main.application.port.`in`.GetTimePatternUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.infrastructure.web.common.WebMvcTestConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(GameFlowStatsWebAdapter::class)
@Import(WebMvcTestConfig::class)
class GameFlowStatsWebAdapterTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getTimePatternUseCase: GetTimePatternUseCase

    @MockitoBean
    private lateinit var getGameLengthTendencyUseCase: GetGameLengthTendencyUseCase

    @MockitoBean
    private lateinit var getEarlyGameDominanceUseCase: GetEarlyGameDominanceUseCase

    @MockitoBean
    private lateinit var getComebackIndexUseCase: GetComebackIndexUseCase

    @MockitoBean
    private lateinit var getObjectiveCorrelationUseCase: GetObjectiveCorrelationUseCase

    /**
     * Jackson 은 Kotlin 의 `is` 접두 Boolean 을 getter 규칙대로 읽어 접두를 떼어 낸다. 실제로 운영
     * 응답이 `"king": true` 로 나가고 있었고, 화면은 `isKing` 을 읽어 **역전왕 표시가 한 번도
     * 뜨지 않았다.** 조용히 틀리는 자리라 이름을 못박는다.
     */
    @Test
    fun `isKing 은 접두가 깎이지 않고 그대로 나간다`() {
        whenever(getComebackIndexUseCase.getComebackIndex(GameMode.NORMAL)).thenReturn(
            ComebackIndexResult(
                rankings =
                    listOf(
                        ComebackIndexEntry(
                            riotId = "가#KR1",
                            totalGames = 20,
                            totalWinRate = 55,
                            contestGames = 8,
                            contestWinRate = 75,
                            comebackBonus = 20,
                            isKing = true,
                        ),
                    ),
                comebackKing = "가#KR1",
                topComebackMatches = emptyList(),
            ),
        )

        mockMvc
            .get("/api/stats/comeback")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.rankings[0].isKing") { value(true) }
                jsonPath("$.data.rankings[0].king") { doesNotExist() }
            }
    }
}
