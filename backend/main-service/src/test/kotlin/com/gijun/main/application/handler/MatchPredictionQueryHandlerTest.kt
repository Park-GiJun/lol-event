package com.gijun.main.application.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.application.dto.query.DescribeMatchPredictionQuery
import com.gijun.main.application.dto.result.TeamCandidateResult
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.external.PlayerTier
import com.gijun.main.application.port.out.external.PlayerTierKtorPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.WinModelQueryPersistencePort
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.match.model.MatchParticipantModel
import com.gijun.main.domain.prediction.model.WinModel
import com.gijun.main.infrastructure.adapter.out.persistence.prediction.WinModelResourceAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MatchPredictionQueryHandlerTest {
    private val lanes = listOf("TOP", "JUNGLE", "MID", "ADC", "SUPPORT")
    private val blueIds = (1..5).map { "블루$it#KR1" }
    private val redIds = (1..5).map { "레드$it#KR1" }

    /** 계수를 읽기 쉬운 값으로 둔다: 자리 라인 승률 1%p 가 logit 0.05, 티어 하나가 logit 1. */
    private val model = WinModel(5.0, 1.0, shrinkPrior = 10.0, tierMean = 4.0, trainedMatches = 160, lastGameCreation = 0L)

    private val matches = mock<MatchQueryPersistencePort>()
    private val tiers = mock<PlayerTierKtorPort>()
    private val cache =
        object : StatsResultCacheQueryPort {
            override fun <T> getOrCompute(
                key: String,
                compute: () -> T,
            ): T = compute()
        }

    private fun handler(known: List<String> = blueIds + redIds) =
        MatchPredictionQueryHandler(
            winModelQueryPersistencePort =
                object : WinModelQueryPersistencePort {
                    override fun find() = model
                },
            getTeamCandidatesUseCase =
                object : GetTeamCandidatesUseCase {
                    override fun getTeamCandidates() = known.map { TeamCandidateResult(it, 1500.0, 10, "TOP", emptyList(), lanes) }
                },
            matchQueryPersistencePort = matches,
            statsResultCacheQueryPort = cache,
            playerTierKtorPort = tiers,
        )

    /** 블루가 다섯 라인을 다 이긴 경기 하나. */
    private fun history() =
        listOf(
            MatchModel(
                matchId = "KR_1",
                queueId = 3130,
                gameCreation = 1L,
                gameDuration = 1_800,
                participants =
                    (
                        lanes.mapIndexed { i, lane -> participant(blueIds[i], 100, lane, win = true, gold = 12_000) } +
                            lanes.mapIndexed { i, lane -> participant(redIds[i], 200, lane, win = false, gold = 10_000) }
                    ).toMutableList(),
            ),
        )

    private fun participant(
        riotId: String,
        teamId: Int,
        position: String,
        win: Boolean,
        gold: Int,
    ) = MatchParticipantModel(
        riotId = riotId,
        champion = "Garen",
        team = if (teamId == 100) "blue" else "red",
        teamId = teamId,
        win = win,
        gold = gold,
        assignedPosition = position,
    )

    private fun ask(
        blue: List<String> = blueIds.map { it.substringBefore('#') },
        red: List<String> = redIds.map { it.substringBefore('#') },
        known: List<String> = blueIds + redIds,
    ) = handler(known).describeMatchPrediction(DescribeMatchPredictionQuery(blue, red))

    @Test
    fun `기록도 티어도 같으면 정확히 반반이다`() {
        whenever(matches.findAllWithParticipants(any())).doReturn(emptyList())

        val text = ask()

        assertTrue(text.startsWith("[승률 예측] 블루 50% · 레드 50% — 거의 비슷하다")) { text }
        assertTrue(text.contains("이 자리 기록 없음")) { text }
        assertTrue(text.contains("참고용")) { text }
    }

    @Test
    fun `라인전 전적과 티어가 앞선 팀의 확률이 모델 식 그대로 나온다`() {
        whenever(matches.findAllWithParticipants(any())).doReturn(history())
        // 블루는 전원 에메랄드 IV(5.0), 레드는 전원 플래티넘 IV(4.0) → 티어 차 1.
        blueIds.forEach { whenever(tiers.findTier(it)).doReturn(PlayerTier("RANKED_SOLO_5x5", "EMERALD", "IV", 0)) }
        redIds.forEach { whenever(tiers.findTier(it)).doReturn(PlayerTier("RANKED_SOLO_5x5", "PLATINUM", "IV", 0)) }

        val text = ask()

        // 자리 라인 승률: 이긴 쪽 (1 + 10 × 6/11) / 11, 진 쪽 (0 + 10 × 5/11) / 11 → 차 21/121.
        val expected = model.blueWinProbability(21.0 / 121.0, 1.0)
        assertTrue(text.startsWith("[승률 예측] 블루 ${Math.round(expected * 100)}% ")) { text }
        assertTrue(text.contains("블루가 유리하다")) { text }
        assertTrue(text.contains("블루가 1.0티어 높다")) { text }
        assertTrue(text.contains("- 탑: 블루 블루1#KR1 (자리 라인 승률 59%, 이 자리 맞대결 1번, EMERALD IV 0LP)")) { text }
    }

    @Test
    fun `두 팀을 맞바꾸면 확률도 맞바뀐다`() {
        whenever(matches.findAllWithParticipants(any())).doReturn(history())

        val asPlayed = ask()
        val swapped = ask(blue = redIds, red = blueIds)

        val blue = Regex("블루 (\\d+)%").find(asPlayed)?.groupValues?.get(1)
        val red = Regex("레드 (\\d+)%").find(swapped)?.groupValues?.get(1)
        assertEquals(blue, red)
    }

    @Test
    fun `티어를 못 받은 사람은 모델의 평균으로 치고 그 사실을 적는다`() {
        whenever(matches.findAllWithParticipants(any())).doReturn(emptyList())
        // 평균(4.0)과 같은 티어를 준 사람과 못 받은 사람이 섞여도 차이는 0 이다.
        whenever(tiers.findTier(blueIds[0])).doReturn(PlayerTier("RANKED_SOLO_5x5", "PLATINUM", "IV", 0))

        val text = ask()

        assertTrue(text.startsWith("[승률 예측] 블루 50% · 레드 50%")) { text }
        assertTrue(text.contains("티어를 확인하지 못해 평균으로 계산한 사람: 블루2#KR1")) { text }
        assertTrue(!text.contains("평균으로 계산한 사람: 블루1#KR1")) { text }
    }

    @Test
    fun `이름을 못 찾으면 예측하지 않고 Riot 도 부르지 않는다`() {
        val text = ask(blue = listOf("없는사람") + blueIds.drop(1))

        assertTrue(text.contains("'없는사람' 이라는 플레이어를 찾지 못했다")) { text }
        assertTrue(!text.contains("승률 예측")) { text }
        verify(tiers, never()).findTier(any())
    }

    @Test
    fun `이름이 여럿에 맞으면 누구인지 되묻는다`() {
        val text = ask(blue = listOf("블루") + blueIds.drop(1))

        assertTrue(text.contains("'블루' 에 맞는 사람이 여럿이다")) { text }
    }

    @Test
    fun `같은 사람이 두 자리에 들어가거나 다섯 명이 아니면 예측하지 않는다`() {
        assertTrue(ask(red = listOf(blueIds[0]) + redIds.drop(1)).contains("같은 사람이 두 번 들어갔다"))
        assertTrue(ask(blue = blueIds.take(4)).contains("팀마다 다섯 명이 필요하다"))
        assertTrue(ask(blue = blueIds.take(4) + " ").contains("팀마다 다섯 명이 필요하다"))
    }

    @Test
    fun `저장소에 커밋된 모델 파일을 읽을 수 있다`() {
        val loaded = WinModelResourceAdapter(ObjectMapper()).find()

        assertTrue(loaded.seatLaneWinRateCoefficient > 0.0 && loaded.tierCoefficient > 0.0) { loaded.toString() }
        assertEquals(10.0, loaded.shrinkPrior)
        assertTrue(loaded.trainedMatches > 0)
        // 기록도 티어도 같으면 반반이어야 한다(bias 가 없다).
        assertEquals(0.5, loaded.blueWinProbability(0.0, 0.0))
    }
}
