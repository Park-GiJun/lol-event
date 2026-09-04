package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.home.result.HomePeriod
import com.gijun.main.application.dto.home.result.HomeResult
import com.gijun.main.application.dto.match.result.MatchSummaryResult
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetHomeUseCase
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetWeeklyAwardsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val TOP_PLAYERS = 10
private const val TOP_CHAMPIONS = 10
private const val RECENT_MATCHES = 5
private const val ELO_MIN_GAMES = 5
private const val TIER_MIN_GAMES = 5

@Service
@Transactional(readOnly = true)
class GetHomeHandler(
    private val getOverviewStatsUseCase: GetOverviewStatsUseCase,
    private val getEloLeaderboardUseCase: GetEloLeaderboardUseCase,
    private val getChampionTierUseCase: GetChampionTierUseCase,
    private val getWeeklyAwardsUseCase: GetWeeklyAwardsUseCase,
    private val matchPersistencePort: MatchPersistencePort,
) : GetHomeUseCase {

    override fun getHome(mode: String): HomeResult {
        val queueIds = modeToQueueIds(mode)
        val recent = matchPersistencePort.findPageWithParticipants(queueIds, 0, RECENT_MATCHES)

        val leaderboard = getEloLeaderboardUseCase.getLeaderboard(ELO_MIN_GAMES)
        val tier = getChampionTierUseCase.getChampionTier(mode, TIER_MIN_GAMES)

        // 기간과 등장 인원은 집계 쿼리로 센다. 이것 하나 때문에 전체 경기를 로드하지 않는다.
        val summary = matchPersistencePort.findPeriodSummary(queueIds)
        val period = HomePeriod(
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
}
