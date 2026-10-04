package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.DragonChampionResult
import com.gijun.main.application.dto.result.TeamCandidateResult
import com.gijun.main.application.port.`in`.GetDragonChampionsUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class RagDescribeQueryHandlerTest {
    private val composer = mock<PlayerProfileComposer> { on { compose(eq("도르비이#KR1"), any()) } doReturn "[플레이어] 도르비이#KR1 (도르비이)" }

    private val handler =
        RagDescribeQueryHandler(
            playerProfileComposer = composer,
            getChampionPageUseCase = mock(),
            getChampionSynergyUseCase = mock(),
            getDragonChampionsUseCase =
                object : GetDragonChampionsUseCase {
                    override fun getDragonChampions() = listOf(DragonChampionResult(105, "Fizz", "피즈", null, null, null))
                },
            getTeamCandidatesUseCase =
                object : GetTeamCandidatesUseCase {
                    override fun getTeamCandidates() =
                        listOf(TeamCandidateResult("도르비이#KR1", 1537.0, 69, "TOP", emptyList(), listOf("TOP")))
                },
            getLaneChampionsUseCase = mock(),
            getAllyPicksUseCase = mock(),
        )

    @Test
    fun `플레이어 이름으로 챔피언을 조회하면 그 플레이어의 기록을 바로 준다`() {
        val text = handler.describeChampion("도르비이")

        assertTrue(text.startsWith("'도르비이' 은 챔피언이 아니라 플레이어다.")) { text }
        assertTrue(text.contains("[플레이어] 도르비이#KR1")) { text }
    }

    @Test
    fun `어느 쪽에도 없는 이름이면 지어내지 말라고 적어 돌려준다`() {
        val champion = handler.describeChampion("없는이름")
        val player = handler.describePlayer("없는이름")

        assertTrue(champion.contains("챔피언도 플레이어도 찾지 못했다")) { champion }
        assertTrue(champion.contains("지어내 제안하지 않는다")) { champion }
        assertTrue(player.contains("플레이어도 챔피언도 찾지 못했다")) { player }
        assertFalse(player.contains("[플레이어]")) { player }
    }
}
