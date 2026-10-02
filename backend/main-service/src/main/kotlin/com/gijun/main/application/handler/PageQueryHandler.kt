package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetChampionMatchupQuery
import com.gijun.main.application.dto.query.GetChampionPageQuery
import com.gijun.main.application.dto.query.GetChampionStatsQuery
import com.gijun.main.application.dto.query.GetChampionTierQuery
import com.gijun.main.application.dto.query.GetDuoStatsQuery
import com.gijun.main.application.dto.query.GetPlayerStatsQuery
import com.gijun.main.application.dto.query.GetPlayerStreakQuery
import com.gijun.main.application.dto.query.GetRivalMatchupQuery
import com.gijun.main.application.dto.query.GetSummonerProfileQuery
import com.gijun.main.application.dto.result.ChampionPageResult
import com.gijun.main.application.dto.result.HomePeriod
import com.gijun.main.application.dto.result.HomeResult
import com.gijun.main.application.dto.result.MatchSummaryResult
import com.gijun.main.application.dto.result.SummonerOpponent
import com.gijun.main.application.dto.result.SummonerProfile
import com.gijun.main.application.dto.result.SummonerProfileResult
import com.gijun.main.application.dto.result.SummonerStreak
import com.gijun.main.application.dto.result.SummonerTeammate
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionPageUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetHomeUseCase
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStreakUseCase
import com.gijun.main.application.port.`in`.GetRivalMatchupUseCase
import com.gijun.main.application.port.`in`.GetSummonerProfileUseCase
import com.gijun.main.application.port.`in`.GetWeeklyAwardsUseCase
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.stats.service.RankingScore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val TOP_PLAYERS = 10
private const val TOP_CHAMPIONS = 10
private const val RECENT_MATCHES = 5

/** 리더보드 순위에 들어가기 위한 최소 라인 맞대결 수. 경기 수가 아니다. */
private const val ELO_MIN_DUELS = 10
private const val TIER_MIN_GAMES = 5

/**
 * 관계 지표는 1경기부터 전부 집계해서 내려보낸다.
 * 어디서 자를지는 화면이 정한다 — 표본 등급을 같이 주므로 판단할 재료는 충분하다.
 */
private const val RELATION_MIN_GAMES = 1

/**
 * 화면 단위 집계 — 홈, 챔피언 화면, 소환사 화면.
 *
 * 화면 한 장이 여러 지표를 같이 보여 줄 때 왕복을 한 번으로 줄이려고 둔다. 지표를 여기서
 * 다시 계산하지 않고 **그 지표를 소유한 유즈케이스를 부른다** — 두 벌로 계산하면 반드시 갈라진다.
 */
@Service
@Transactional(readOnly = true)
class PageQueryHandler(
    private val getOverviewStatsUseCase: GetOverviewStatsUseCase,
    private val getEloLeaderboardUseCase: GetEloLeaderboardUseCase,
    private val getChampionTierUseCase: GetChampionTierUseCase,
    private val getWeeklyAwardsUseCase: GetWeeklyAwardsUseCase,
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val getChampionStatsUseCase: GetChampionStatsUseCase,
    private val getChampionMatchupUseCase: GetChampionMatchupUseCase,
    private val getPlayerStatsUseCase: GetPlayerStatsUseCase,
    private val getPlayerStreakUseCase: GetPlayerStreakUseCase,
    private val getDuoStatsUseCase: GetDuoStatsUseCase,
    private val getRivalMatchupUseCase: GetRivalMatchupUseCase,
) : GetHomeUseCase,
    GetChampionPageUseCase,
    GetSummonerProfileUseCase {
    override fun getHome(mode: GameMode): HomeResult {
        val queueIds = mode.queueIds
        val recent = matchQueryPersistencePort.findPageWithParticipants(queueIds, 0, RECENT_MATCHES)

        val leaderboard = getEloLeaderboardUseCase.getEloLeaderboard(ELO_MIN_DUELS)
        val tier = getChampionTierUseCase.getChampionTier(GetChampionTierQuery(mode, TIER_MIN_GAMES))

        // 기간과 등장 인원은 집계 쿼리로 센다. 이것 하나 때문에 전체 경기를 로드하지 않는다.
        val summary = matchQueryPersistencePort.findPeriodSummary(queueIds)
        val period =
            HomePeriod(
                firstMatchAt = summary.firstMatchAt,
                lastMatchAt = summary.lastMatchAt,
                totalMatches = summary.totalMatches.toInt(),
                playerCount = summary.playerCount.toInt(),
            )

        return HomeResult(
            overview = getOverviewStatsUseCase.getOverviewStats(mode),
            // 배치 중(rank=0)은 홈에 올리지 않는다.
            topPlayers = leaderboard.players.filter { !it.placement }.take(TOP_PLAYERS),
            // 표본 미달은 정렬상 뒤로 밀려 있으므로 앞에서 자르면 자연히 제외된다.
            topChampions = tier.tierList.filter { it.games >= TIER_MIN_GAMES }.take(TOP_CHAMPIONS),
            awards = getWeeklyAwardsUseCase.getWeeklyAwards(mode),
            recentMatches = recent.map { MatchSummaryResult.from(it) },
            period = period,
        )
    }

    override fun getChampionPage(query: GetChampionPageQuery): ChampionPageResult {
        val detail = getChampionStatsUseCase.getChampionStats(GetChampionStatsQuery(query.champion, query.mode))

        val tier =
            getChampionTierUseCase
                .getChampionTier(GetChampionTierQuery(query.mode, TIER_MIN_GAMES))
                .tierList
                .firstOrNull { it.champion == query.champion }

        val matchup =
            getChampionMatchupUseCase
                .getChampionMatchup(GetChampionMatchupQuery(champion = query.champion, vsChampion = null, mode = query.mode))

        // 챔피언 시너지는 걷어냈다. 154경기에서 챔피언 2인 조합은 2,142가지가 나오는데
        // 중앙 표본이 1회, 10회 이상은 단 하나뿐이라 어떤 컷을 걸어도 의미가 생기지 않는다.
        return ChampionPageResult(
            detail = detail,
            tier = tier,
            laneStrength = matchup.laneStrength,
            matchups = matchup.matchups,
            matchupMinGames = matchup.minGames,
        )
    }

    override fun getSummonerProfile(query: GetSummonerProfileQuery): SummonerProfileResult {
        val detail = getPlayerStatsUseCase.getPlayerStats(GetPlayerStatsQuery(query.riotId, query.mode))
        val streak = getPlayerStreakUseCase.getPlayerStreak(GetPlayerStreakQuery(query.riotId, query.mode))
        val leaderboard = getEloLeaderboardUseCase.getEloLeaderboard(ELO_MIN_DUELS)

        val myRating = leaderboard.players.firstOrNull { it.riotId == query.riotId.value }
        val myRank = myRating?.takeIf { !it.placement }?.rank

        val profile =
            SummonerProfile(
                riotId = detail.riotId,
                games = detail.games,
                wins = detail.wins,
                losses = detail.losses,
                winRate = detail.winRate,
                adjustedWinRate = round2(RankingScore.shrunkWinRate(detail.wins, detail.games)),
                sampleGrade = RankingScore.sampleGrade(detail.games),
                kda = detail.kda,
                avgKills = detail.avgKills,
                avgDeaths = detail.avgDeaths,
                avgAssists = detail.avgAssists,
                avgDamage = detail.avgDamage,
                avgCs = detail.avgCs,
                avgGold = detail.avgGold,
                avgVisionScore = detail.avgVisionScore,
                elo = detail.elo,
                eloRank = myRank,
                eloRankedTotal = leaderboard.rankedCount,
                rating = myRating,
            )

        return SummonerProfileResult(
            profile = profile,
            streak =
                SummonerStreak(
                    current = streak.currentStreak,
                    type = streak.currentStreakType,
                    longestWin = streak.longestWinStreak,
                    longestLoss = streak.longestLossStreak,
                    recentForm = streak.recentForm,
                ),
            championStats = detail.championStats,
            positionStats = detail.laneStats,
            recentMatches = detail.recentMatches,
            teammates = teammatesOf(query.riotId.value, query.mode),
            opponents = opponentsOf(query.riotId.value, query.mode),
        )
    }

    /**
     * 듀오 통계에서 이 소환사가 낀 쌍만 골라 상대방 관점으로 뒤집는다.
     * 같은 팀이라 승패는 둘이 공유하므로 wins 를 그대로 쓸 수 있다.
     */
    private fun teammatesOf(
        riotId: String,
        mode: GameMode,
    ): List<SummonerTeammate> =
        getDuoStatsUseCase
            .getDuoStats(GetDuoStatsQuery(mode, RELATION_MIN_GAMES))
            .duos
            .filter { it.player1 == riotId || it.player2 == riotId }
            .map { duo ->
                SummonerTeammate(
                    riotId = if (duo.player1 == riotId) duo.player2 else duo.player1,
                    games = duo.games,
                    wins = duo.wins,
                    winRate = duo.winRate,
                    adjustedWinRate = duo.adjustedWinRate,
                    sampleGrade = duo.sampleGrade,
                )
            }.sortedByDescending { it.games }

    /**
     * 라이벌 전적은 player1 관점으로 저장돼 있다.
     * 이 소환사가 player2 쪽이면 승패를 뒤집어야 한다.
     */
    private fun opponentsOf(
        riotId: String,
        mode: GameMode,
    ): List<SummonerOpponent> =
        getRivalMatchupUseCase
            .getRivalMatchup(GetRivalMatchupQuery(mode, RELATION_MIN_GAMES))
            .rivalries
            .filter { it.player1 == riotId || it.player2 == riotId }
            .map { rival ->
                val isPlayer1 = rival.player1 == riotId
                val wins = if (isPlayer1) rival.player1Wins else rival.player2Wins
                val losses = if (isPlayer1) rival.player2Wins else rival.player1Wins
                SummonerOpponent(
                    riotId = if (isPlayer1) rival.player2 else rival.player1,
                    games = rival.games,
                    wins = wins,
                    losses = losses,
                    winRate = if (rival.games > 0) wins * 100 / rival.games else 0,
                    adjustedWinRate = round2(RankingScore.shrunkWinRate(wins, rival.games)),
                    sampleGrade = RankingScore.sampleGrade(rival.games),
                )
            }.sortedByDescending { it.games }

    private fun round2(v: Double) = (v * 100).toInt() / 100.0
}
