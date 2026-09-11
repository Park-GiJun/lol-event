package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.summoner.result.SummonerOpponent
import com.gijun.main.application.dto.summoner.result.SummonerProfile
import com.gijun.main.application.dto.summoner.result.SummonerProfileResult
import com.gijun.main.application.dto.summoner.result.SummonerStreak
import com.gijun.main.application.dto.summoner.result.SummonerTeammate
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStreakUseCase
import com.gijun.main.application.port.`in`.GetRivalMatchupUseCase
import com.gijun.main.application.port.`in`.GetSummonerProfileUseCase
import com.gijun.main.domain.service.RankingScore
import com.gijun.main.domain.service.RiotIdNormalizer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 리더보드 순위에 들어가기 위한 최소 라인 맞대결 수. 경기 수가 아니다.
 * RankingStatsWebAdapter 기본값과 맞춘다.
 */
private const val ELO_MIN_DUELS = 10

/**
 * 관계 지표는 1경기부터 전부 집계해서 내려보낸다.
 * 어디서 자를지는 화면이 정한다 — 표본 등급을 같이 주므로 판단할 재료는 충분하다.
 */
private const val RELATION_MIN_GAMES = 1

@Service
@Transactional(readOnly = true)
class GetSummonerProfileHandler(
    private val getPlayerStatsUseCase: GetPlayerStatsUseCase,
    private val getPlayerStreakUseCase: GetPlayerStreakUseCase,
    private val getEloLeaderboardUseCase: GetEloLeaderboardUseCase,
    private val getDuoStatsUseCase: GetDuoStatsUseCase,
    private val getRivalMatchupUseCase: GetRivalMatchupUseCase,
    private val normalizer: RiotIdNormalizer,
) : GetSummonerProfileUseCase {

    override fun getProfile(riotId: String, mode: String): SummonerProfileResult {
        val detail = getPlayerStatsUseCase.getPlayerStats(riotId, mode)
        val streak = getPlayerStreakUseCase.getPlayerStreak(riotId, mode)
        val leaderboard = getEloLeaderboardUseCase.getLeaderboard(ELO_MIN_DUELS)

        // 리더보드는 정규 이름으로 키를 잡는다. 부계정 이름으로 들어온 주소도 같은 줄을 찾아야 한다.
        val canonicalId = normalizer.canonical(riotId)
        val myRating = leaderboard.players.firstOrNull { it.riotId == canonicalId }
        val myRank = myRating?.takeIf { !it.placement }?.rank

        val profile = SummonerProfile(
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
            streak = SummonerStreak(
                current = streak.currentStreak,
                type = streak.currentStreakType,
                longestWin = streak.longestWinStreak,
                longestLoss = streak.longestLossStreak,
                recentForm = streak.recentForm,
            ),
            championStats = detail.championStats,
            positionStats = detail.laneStats,
            recentMatches = detail.recentMatches,
            teammates = teammatesOf(riotId, mode),
            opponents = opponentsOf(riotId, mode),
        )
    }

    /**
     * 듀오 통계에서 이 소환사가 낀 쌍만 골라 상대방 관점으로 뒤집는다.
     * 같은 팀이라 승패는 둘이 공유하므로 wins 를 그대로 쓸 수 있다.
     */
    private fun teammatesOf(riotId: String, mode: String): List<SummonerTeammate> =
        getDuoStatsUseCase.getDuoStats(mode, RELATION_MIN_GAMES).duos
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
            }
            .sortedByDescending { it.games }

    /**
     * 라이벌 전적은 player1 관점으로 저장돼 있다.
     * 이 소환사가 player2 쪽이면 승패를 뒤집어야 한다.
     */
    private fun opponentsOf(riotId: String, mode: String): List<SummonerOpponent> =
        getRivalMatchupUseCase.getRivalMatchups(mode, RELATION_MIN_GAMES).rivalries
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
            }
            .sortedByDescending { it.games }

    private fun round2(v: Double) = (v * 100).toInt() / 100.0
}
