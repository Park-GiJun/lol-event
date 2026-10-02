package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetDuoStatsQuery
import com.gijun.main.application.dto.query.GetPlayerStatsQuery
import com.gijun.main.application.dto.query.GetPlayerStreakQuery
import com.gijun.main.application.dto.query.GetRivalMatchupQuery
import com.gijun.main.application.dto.query.GetSummonerProfileQuery
import com.gijun.main.application.dto.result.DuoStat
import com.gijun.main.application.dto.result.DuoStatsResult
import com.gijun.main.application.dto.result.EloLeaderboardResult
import com.gijun.main.application.dto.result.EloRankEntry
import com.gijun.main.application.dto.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.result.RivalMatchupEntry
import com.gijun.main.application.dto.result.RivalMatchupResult
import com.gijun.main.application.dto.result.StreakResult
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStreakUseCase
import com.gijun.main.application.port.`in`.GetRivalMatchupUseCase
import com.gijun.main.application.port.`in`.GetWeeklyAwardsUseCase
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.stats.service.RankingScore
import com.gijun.main.shared.domain.vo.RiotId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 라이벌 전적은 player1 관점 한 방향으로만 저장된다.
 * 조회하는 소환사가 player2 쪽에 있으면 승패를 뒤집어야 하는데,
 * 뒤집기를 빠뜨려도 화면에는 그럴듯한 숫자가 나와서 눈으로는 못 잡는다.
 */
class PageQueryHandlerTest {
    private val me = "나#KR1"
    private val other = "상대#KR2"

    private fun handler(
        rivalries: List<RivalMatchupEntry> = emptyList(),
        duos: List<DuoStat> = emptyList(),
        leaderboard: List<EloRankEntry> = emptyList(),
        rankedCount: Int = 0,
    ) = PageQueryHandler(
        getPlayerStatsUseCase =
            object : GetPlayerStatsUseCase {
                override fun getPlayerStats(query: GetPlayerStatsQuery) =
                    PlayerDetailStatsResult(
                        riotId = query.riotId.value,
                        games = 20,
                        wins = 12,
                        losses = 8,
                        winRate = 60,
                        avgKills = 5.0,
                        avgDeaths = 4.0,
                        avgAssists = 6.0,
                        kda = 2.75,
                        avgDamage = 20000,
                        avgCs = 180.0,
                        avgGold = 12000,
                        avgVisionScore = 20.0,
                        elo = 1100.0,
                        eloRank = null,
                        championStats = emptyList(),
                        recentMatches = emptyList(),
                    )
            },
        getPlayerStreakUseCase =
            object : GetPlayerStreakUseCase {
                override fun getPlayerStreak(query: GetPlayerStreakQuery) =
                    StreakResult(
                        riotId = query.riotId.value,
                        currentStreak = 2,
                        currentStreakType = "WIN",
                        longestWinStreak = 5,
                        longestLossStreak = 3,
                        recentForm = listOf("W", "W", "L"),
                        totalGames = 20,
                        wins = 12,
                        losses = 8,
                    )
            },
        getEloLeaderboardUseCase =
            object : GetEloLeaderboardUseCase {
                override fun getEloLeaderboard(minDuels: Int) = EloLeaderboardResult(leaderboard, minDuels, rankedCount, 0)
            },
        getDuoStatsUseCase =
            object : GetDuoStatsUseCase {
                override fun getDuoStats(query: GetDuoStatsQuery) = DuoStatsResult(duos)
            },
        getRivalMatchupUseCase =
            object : GetRivalMatchupUseCase {
                override fun getRivalMatchup(query: GetRivalMatchupQuery) = RivalMatchupResult(rivalries, rivalries.firstOrNull())
            },
        // 소환사 화면은 아래 협력자를 부르지 않는다. 홈·챔피언 화면용이다.
        getOverviewStatsUseCase = Mockito.mock(GetOverviewStatsUseCase::class.java),
        getChampionTierUseCase = Mockito.mock(GetChampionTierUseCase::class.java),
        getWeeklyAwardsUseCase = Mockito.mock(GetWeeklyAwardsUseCase::class.java),
        getChampionStatsUseCase = Mockito.mock(GetChampionStatsUseCase::class.java),
        getChampionMatchupUseCase = Mockito.mock(GetChampionMatchupUseCase::class.java),
        matchQueryPersistencePort = Mockito.mock(MatchQueryPersistencePort::class.java),
    )

    private fun rival(
        p1: String,
        p2: String,
        games: Int,
        p1Wins: Int,
    ) = RivalMatchupEntry(
        player1 = p1,
        player2 = p2,
        games = games,
        player1Wins = p1Wins,
        player2Wins = games - p1Wins,
        player1WinRate = p1Wins * 100 / games,
        player1AdjustedWinRate = RankingScore.shrunkWinRate(p1Wins, games),
        sampleGrade = RankingScore.sampleGrade(games),
    )

    @Test
    fun `내가 player1 이면 전적을 그대로 쓴다`() {
        val result =
            handler(rivalries = listOf(rival(me, other, games = 10, p1Wins = 7)))
                .getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        val opponent = result.opponents.single()
        assertEquals(other, opponent.riotId)
        assertEquals(7, opponent.wins)
        assertEquals(3, opponent.losses)
        assertEquals(70, opponent.winRate)
    }

    @Test
    fun `내가 player2 이면 승패를 뒤집는다`() {
        // 저장된 방향은 상대 관점 7승 3패. 내 화면에서는 3승 7패여야 한다.
        val result =
            handler(rivalries = listOf(rival(other, me, games = 10, p1Wins = 7)))
                .getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        val opponent = result.opponents.single()
        assertEquals(other, opponent.riotId)
        assertEquals(3, opponent.wins)
        assertEquals(7, opponent.losses)
        assertEquals(30, opponent.winRate)
    }

    @Test
    fun `나와 무관한 쌍은 걸러낸다`() {
        val result =
            handler(
                rivalries =
                    listOf(
                        rival(me, other, games = 4, p1Wins = 2),
                        rival("남1#KR1", "남2#KR1", games = 9, p1Wins = 5),
                    ),
            ).getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        assertEquals(listOf(other), result.opponents.map { it.riotId })
    }

    @Test
    fun `상대는 맞붙은 경기 수가 많은 순으로 정렬한다`() {
        val result =
            handler(
                rivalries =
                    listOf(
                        rival(me, "적음#KR1", games = 2, p1Wins = 1),
                        rival("많음#KR1", me, games = 20, p1Wins = 10),
                        rival(me, "중간#KR1", games = 8, p1Wins = 4),
                    ),
            ).getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        assertEquals(listOf("많음#KR1", "중간#KR1", "적음#KR1"), result.opponents.map { it.riotId })
    }

    @Test
    fun `팀원은 어느 쪽에 있든 상대방 이름으로 담고 승패는 공유한다`() {
        val duo =
            DuoStat(
                player1 = other,
                player2 = me,
                games = 12,
                wins = 9,
                winRate = 75,
                adjustedWinRate = 68.18,
                sampleGrade = "MEDIUM",
                avgKills = 10.0,
                avgDeaths = 8.0,
                avgAssists = 12.0,
                kda = 2.75,
            )
        val result = handler(duos = listOf(duo)).getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        val teammate = result.teammates.single()
        assertEquals(other, teammate.riotId)
        assertEquals(9, teammate.wins)
        assertEquals(75, teammate.winRate)
    }

    /** 리더보드 한 줄. 이 테스트가 보는 건 rank / riotId / placement 뿐이라 나머지는 그럴듯한 값으로 채운다. */
    private fun rankEntry(
        rank: Int,
        riotId: String,
        laneElo: Double,
        laneDuels: Int,
        placement: Boolean,
        sampleGrade: String,
    ) = EloRankEntry(
        rank = rank,
        riotId = riotId,
        laneElo = laneElo,
        laneEloDisplay = laneElo,
        laneDuels = laneDuels,
        laneWins = laneDuels / 2,
        laneLosses = laneDuels - laneDuels / 2,
        laneWinRate = 0.5,
        teamElo = laneElo,
        teamEloDisplay = laneElo,
        teamGames = laneDuels,
        winRate = 0.5,
        gap = 0.0,
        placement = placement,
        mainPosition = "MID",
        elo = laneElo,
        games = laneDuels,
        wins = laneDuels / 2,
        losses = laneDuels - laneDuels / 2,
        winStreak = 0,
        lossStreak = 0,
        sampleGrade = sampleGrade,
    )

    @Test
    fun `배치 중이면 Elo 순위를 주지 않는다`() {
        val result =
            handler(
                leaderboard = listOf(rankEntry(0, me, 1510.0, 2, placement = true, sampleGrade = "INSUFFICIENT")),
                rankedCount = 0,
            ).getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        assertNull(result.profile.eloRank)
    }

    @Test
    fun `개인 화면에 리더보드와 같은 레이팅 줄이 그대로 실린다`() {
        // 개인 화면과 리더보드가 다른 숫자를 보여 주면 안 된다. 계산을 두 벌 두지 않고
        // 리더보드 항목을 그대로 싣는 이유다.
        val mine = rankEntry(2, me, 1620.0, 24, placement = false, sampleGrade = "MEDIUM")
        val result =
            handler(
                leaderboard = listOf(rankEntry(1, "1등#KR1", 1700.0, 40, placement = false, sampleGrade = "HIGH"), mine),
                rankedCount = 2,
            ).getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        assertEquals(mine, result.profile.rating)
    }

    @Test
    fun `배치 중이어도 레이팅 줄은 내려준다 — 순위만 없다`() {
        val mine = rankEntry(0, me, 1510.0, 2, placement = true, sampleGrade = "INSUFFICIENT")
        val result =
            handler(
                leaderboard = listOf(mine),
                rankedCount = 0,
            ).getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        assertNull(result.profile.eloRank)
        assertEquals(2, result.profile.rating?.laneDuels)
    }

    @Test
    fun `한 경기도 반영되지 않은 사람은 레이팅 줄이 없다`() {
        val result =
            handler(
                leaderboard = emptyList(),
                rankedCount = 0,
            ).getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        assertNull(result.profile.rating)
        assertNull(result.profile.eloRank)
    }

    @Test
    fun `순위에 든 경우 전체 인원과 함께 순위를 준다`() {
        val result =
            handler(
                leaderboard =
                    listOf(
                        rankEntry(1, "1등#KR1", 1700.0, 40, placement = false, sampleGrade = "HIGH"),
                        rankEntry(2, me, 1600.0, 20, placement = false, sampleGrade = "MEDIUM"),
                    ),
                rankedCount = 2,
            ).getSummonerProfile(GetSummonerProfileQuery(RiotId(me), GameMode.ALL))

        assertEquals(2, result.profile.eloRank)
        assertEquals(2, result.profile.eloRankedTotal)
    }
}
