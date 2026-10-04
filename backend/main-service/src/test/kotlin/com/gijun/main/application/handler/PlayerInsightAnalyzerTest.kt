package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.ChampionStat
import com.gijun.main.application.dto.result.EloRankEntry
import com.gijun.main.application.dto.result.LaneLeaderboardResult
import com.gijun.main.application.dto.result.LaneStat
import com.gijun.main.application.dto.result.PlayerLaneStat
import com.gijun.main.application.dto.result.SummonerProfile
import com.gijun.main.application.dto.result.SummonerProfileResult
import com.gijun.main.application.dto.result.SummonerStreak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerInsightAnalyzerTest {
    private val names = ChampionNames(mapOf("Sion" to "사이온", "Chogath" to "초가스"))

    @Test
    fun `같은 포지션 사람들 사이의 순위로 강점과 약점을 가른다`() {
        // 탑 9 명. 나는 딜량 1 위, 데스는 가장 많다.
        val peers = (1..8).map { lane("p$it", damage = 15_000 + it * 500, deaths = 3.0 + it * 0.2) }
        val me = lane("나#KR1", damage = 30_000, deaths = 9.0)

        val insights = PlayerInsightAnalyzer.analyze(profile("나#KR1"), LaneLeaderboardResult("TOP", peers + me), names)

        assertTrue(insights.strengths.contains("평균 딜량 30,000 — 탑 9명 중 1위")) { insights.strengths.toString() }
        assertTrue(insights.weaknesses.contains("평균 데스 9.0 — 탑 9명 중 9위")) { insights.weaknesses.toString() }
    }

    @Test
    fun `가운데 순위는 말하지 않는다`() {
        val peers = (1..8).map { lane("p$it", damage = 15_000 + it * 500) }
        val me = lane("나#KR1", damage = 17_200)

        val insights = PlayerInsightAnalyzer.analyze(profile("나#KR1"), LaneLeaderboardResult("TOP", peers + me), names)

        assertFalse((insights.strengths + insights.weaknesses).any { it.startsWith("평균 딜량") })
    }

    @Test
    fun `포지션에 뜻이 없는 지표는 견주지 않는다`() {
        // 서포터의 딜량·CS 가 낮은 것은 약점이 아니다.
        val peers = (1..8).map { lane("p$it", damage = 10_000 + it * 500) }
        val me = lane("나#KR1", damage = 3_000)

        val insights = PlayerInsightAnalyzer.analyze(profile("나#KR1"), LaneLeaderboardResult("SUPPORT", peers + me), names)

        assertFalse(insights.weaknesses.any { it.startsWith("평균 딜량") || it.startsWith("평균 CS") }) { insights.weaknesses.toString() }
    }

    @Test
    fun `몇 판 안 한 사람과는 견주지 않고, 나도 판수가 모자라면 견주지 않는다`() {
        val veterans = (1..5).map { lane("p$it", damage = 20_000) }
        val oneGameWonder = lane("한판#KR1", damage = 90_000, games = 1)
        val me = lane("나#KR1", damage = 25_000)

        val ranked = PlayerInsightAnalyzer.analyze(profile("나#KR1"), LaneLeaderboardResult("TOP", veterans + oneGameWonder + me), names)
        assertTrue(ranked.strengths.contains("평균 딜량 25,000 — 탑 6명 중 1위")) { ranked.strengths.toString() }

        val newcomer = lane("신입#KR1", damage = 99_000, games = 2)
        val unranked = PlayerInsightAnalyzer.analyze(profile("신입#KR1"), LaneLeaderboardResult("TOP", veterans + newcomer), names)
        assertFalse(unranked.strengths.any { it.contains("명 중") })
    }

    @Test
    fun `라인 Elo 순위와 포지션 · 챔피언 성적을 근거와 함께 적는다`() {
        val result =
            profile(
                "아랑택#아랑택",
                eloRank = 43,
                eloTotal = 45,
                positions = listOf(position("TOP", 53, 41), position("JUNGLE", 18, 60), position("ADC", 14, 28)),
                champions = listOf(champion("Sion", 28, 57), champion("Chogath", 8, 25)),
            )

        val insights = PlayerInsightAnalyzer.analyze(result, null, names)

        assertTrue(insights.weaknesses.first().startsWith("라인 Elo 1403 — 45명 중 43위")) { insights.weaknesses.toString() }
        assertTrue(insights.weaknesses.contains("원딜에서 14판 승률 28%"))
        assertTrue(insights.weaknesses.contains("탑에서 53판 승률 41%"))
        assertTrue(insights.strengths.contains("정글에서 18판 승률 60%"))
        assertTrue(insights.strengths.contains("직접 플레이한 사이온(Sion) 28판 승률 57% (KDA 2.5)"))
        assertTrue(insights.weaknesses.contains("직접 플레이한 초가스(Chogath) 8판 승률 25% (KDA 2.5)"))
    }

    @Test
    fun `글에는 강점과 약점 줄이 항상 있다`() {
        val text = RagDocumentWriter.playerProfile(profile("나#KR1"), names, PlayerInsights(listOf("KDA 5.00 — 탑 9명 중 1위"), emptyList()))

        assertTrue(text.contains("강점: KDA 5.00 — 탑 9명 중 1위.")) { text }
        // 약점이 없다고 줄을 빼면 모델이 빈자리를 지어내 채운다. 없다는 것도 사실로 적는다.
        assertTrue(text.contains("약점: 같은 포지션 사람들과 견줘 두드러지는 수치가 없다")) { text }
        assertEquals(1, text.lines().count { it.startsWith("강점:") })
    }

    private fun lane(
        riotId: String,
        damage: Int = 20_000,
        deaths: Double = 5.0,
        games: Int = 20,
    ) = PlayerLaneStat(
        riotId = riotId,
        games = games,
        wins = games / 2,
        winRate = 50,
        avgKills = 5.0,
        avgDeaths = deaths,
        avgAssists = 5.0,
        kda = 2.0,
        avgDamage = damage,
        avgCs = 200.0,
        avgGold = 12_000,
        avgVisionScore = 25.0,
        avgDamageTaken = 20_000,
        avgObjectiveDamage = 5_000,
        avgWardsPlaced = 10.0,
        avgCcTime = 20.0,
        avgNeutralMinions = 2.0,
        topChampion = null,
        topChampionId = null,
    )

    private fun position(
        position: String,
        games: Int,
        winRate: Int,
    ) = LaneStat(
        position,
        games,
        games * winRate / 100,
        winRate,
        5.0,
        5.0,
        5.0,
        2.0,
        20_000,
        200.0,
        12_000,
        25.0,
        20_000,
        5_000,
        10.0,
        20.0,
        2.0,
    )

    private fun champion(
        key: String,
        games: Int,
        winRate: Int,
    ) = ChampionStat(key, 1, games, games * winRate / 100, winRate, 5.0, 5.0, 5.0, 2.5, 20_000, 200.0, 12_000)

    private fun profile(
        riotId: String,
        eloRank: Int? = null,
        eloTotal: Int = 0,
        positions: List<LaneStat> = emptyList(),
        champions: List<ChampionStat> = emptyList(),
    ) = SummonerProfileResult(
        profile =
            SummonerProfile(
                riotId = riotId,
                games = 113,
                wins = 48,
                losses = 65,
                winRate = 42,
                adjustedWinRate = 43.0,
                sampleGrade = "HIGH",
                kda = 2.02,
                avgKills = 4.6,
                avgDeaths = 5.8,
                avgAssists = 7.0,
                avgDamage = 24_031,
                avgCs = 206.4,
                avgGold = 12_482,
                avgVisionScore = 28.9,
                elo = 1403.0,
                eloRank = eloRank,
                eloRankedTotal = eloTotal,
                rating =
                    eloRank?.let {
                        EloRankEntry(
                            rank = it,
                            riotId = riotId,
                            laneElo = 1403.27,
                            laneEloDisplay = 1411.0,
                            laneDuels = 106,
                            laneWins = 40,
                            laneLosses = 66,
                            laneWinRate = 0.377,
                            teamElo = 1421.9,
                            teamEloDisplay = 1428.0,
                            teamGames = 111,
                            winRate = 0.441,
                            gap = 18.6,
                            placement = false,
                            mainPosition = "TOP",
                            elo = 1403.27,
                            games = 111,
                            wins = 49,
                            losses = 62,
                            winStreak = 0,
                            lossStreak = 2,
                            sampleGrade = "HIGH",
                        )
                    },
            ),
        streak = SummonerStreak(0, "NONE", 0, 0, emptyList()),
        championStats = champions,
        positionStats = positions,
        recentMatches = emptyList(),
        teammates = emptyList(),
        opponents = emptyList(),
    )
}
