package com.gijun.main.infrastructure.adapter.`in`.web.page.dto

import com.gijun.main.application.dto.result.ChampionPageResult
import com.gijun.main.application.dto.result.HomePeriod
import com.gijun.main.application.dto.result.HomeResult
import com.gijun.main.application.dto.result.SummonerOpponent
import com.gijun.main.application.dto.result.SummonerProfile
import com.gijun.main.application.dto.result.SummonerProfileResult
import com.gijun.main.application.dto.result.SummonerStreak
import com.gijun.main.application.dto.result.SummonerTeammate
import com.gijun.main.infrastructure.adapter.`in`.web.match.dto.MatchSummaryResponse
import com.gijun.main.infrastructure.adapter.`in`.web.rating.dto.EloRankEntryResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChampionDetailStatsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChampionLaneStrengthResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChampionStatResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChampionTierEntryResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.LaneStatResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.MatchupStatResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.OverviewStatsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.RecentMatchStatResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.WeeklyAwardsResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "HomeResult")
data class HomeResponse(
    val overview: OverviewStatsResponse,
    @field:Schema(description = "Elo 상위. 배치 중인 플레이어는 들어가지 않는다.")
    val topPlayers: List<EloRankEntryResponse>,
    @field:Schema(description = "표본을 충족한 챔피언만. 티어표 상위.")
    val topChampions: List<ChampionTierEntryResponse>,
    val awards: WeeklyAwardsResponse,
    val recentMatches: List<MatchSummaryResponse>,
    val period: HomePeriodResponse,
) {
    companion object {
        fun from(result: HomeResult) =
            HomeResponse(
                overview = OverviewStatsResponse.from(result.overview),
                topPlayers = result.topPlayers.map(EloRankEntryResponse::from),
                topChampions = result.topChampions.map(ChampionTierEntryResponse::from),
                awards = WeeklyAwardsResponse.from(result.awards),
                recentMatches = result.recentMatches.map(MatchSummaryResponse::from),
                period = HomePeriodResponse.from(result.period),
            )
    }
}

@Schema(name = "HomePeriod")
data class HomePeriodResponse(
    val firstMatchAt: Long?,
    val lastMatchAt: Long?,
    val totalMatches: Int,
    @field:Schema(description = "경기에 한 번이라도 등장한 인원")
    val playerCount: Int,
) {
    companion object {
        fun from(result: HomePeriod) =
            HomePeriodResponse(
                firstMatchAt = result.firstMatchAt,
                lastMatchAt = result.lastMatchAt,
                totalMatches = result.totalMatches,
                playerCount = result.playerCount,
            )
    }
}

@Schema(name = "ChampionPageResult")
data class ChampionPageResponse(
    val detail: ChampionDetailStatsResponse,
    @field:Schema(description = "티어표에서 이 챔피언 항목. 표본 미달이면 tier 가 \"?\" 다.")
    val tier: ChampionTierEntryResponse?,
    @field:Schema(description = "라인별 라인전 지표. 상대 라이너 대비 골드·CS·딜량 격차가 들어 있다. 개별 상성보다 표본이 두터워 이쪽이 실제로 읽히는 값이다.")
    val laneStrength: List<ChampionLaneStrengthResponse>,
    @field:Schema(description = "같은 라인에서 맞붙은 상대별 전적. 최소 표본을 넘긴 것만.")
    val matchups: List<MatchupStatResponse>,
    @field:Schema(description = "matchups 에 적용된 최소 표본.")
    val matchupMinGames: Int,
) {
    companion object {
        fun from(result: ChampionPageResult) =
            ChampionPageResponse(
                detail = ChampionDetailStatsResponse.from(result.detail),
                tier = result.tier?.let(ChampionTierEntryResponse::from),
                laneStrength = result.laneStrength.map(ChampionLaneStrengthResponse::from),
                matchups = result.matchups.map(MatchupStatResponse::from),
                matchupMinGames = result.matchupMinGames,
            )
    }
}

@Schema(name = "SummonerProfileResult")
data class SummonerProfileResponse(
    val profile: SummonerProfileCardResponse,
    val streak: SummonerStreakResponse,
    val championStats: List<ChampionStatResponse>,
    val positionStats: List<LaneStatResponse>,
    val recentMatches: List<RecentMatchStatResponse>,
    @field:Schema(description = "같은 팀으로 함께 뛴 사람들. 함께한 경기 수 순.")
    val teammates: List<SummonerTeammateResponse>,
    @field:Schema(description = "상대 팀으로 만난 사람들. 맞붙은 경기 수 순.")
    val opponents: List<SummonerOpponentResponse>,
) {
    companion object {
        fun from(result: SummonerProfileResult) =
            SummonerProfileResponse(
                profile = SummonerProfileCardResponse.from(result.profile),
                streak = SummonerStreakResponse.from(result.streak),
                championStats = result.championStats.map(ChampionStatResponse::from),
                positionStats = result.positionStats.map(LaneStatResponse::from),
                recentMatches = result.recentMatches.map(RecentMatchStatResponse::from),
                teammates = result.teammates.map(SummonerTeammateResponse::from),
                opponents = result.opponents.map(SummonerOpponentResponse::from),
            )
    }
}

@Schema(name = "SummonerProfile")
data class SummonerProfileCardResponse(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val adjustedWinRate: Double,
    val sampleGrade: String,
    val kda: Double,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
    @field:Schema(description = "실력 레이팅 표시값. 리더보드에 찍히는 것과 같은 숫자다.")
    val elo: Double,
    @field:Schema(description = "배치 중이면 null")
    val eloRank: Int?,
    @field:Schema(description = "순위가 매겨진 전체 인원. \"14위 / 41명\" 처럼 쓰라고 같이 준다.")
    val eloRankedTotal: Int,
    @field:Schema(
        description =
            "이 사람의 두 레이팅 한 줄. 리더보드 항목을 그대로 싣는다 — 개인 화면과 리더보드가 다른 숫자를 보여 주면 안 되므로 계산을 두 벌 두지 않는다. 한 경기도 반영되지 " +
                "않은 사람은 null 이다.",
    )
    val rating: EloRankEntryResponse?,
) {
    companion object {
        fun from(result: SummonerProfile) =
            SummonerProfileCardResponse(
                riotId = result.riotId,
                games = result.games,
                wins = result.wins,
                losses = result.losses,
                winRate = result.winRate,
                adjustedWinRate = result.adjustedWinRate,
                sampleGrade = result.sampleGrade,
                kda = result.kda,
                avgKills = result.avgKills,
                avgDeaths = result.avgDeaths,
                avgAssists = result.avgAssists,
                avgDamage = result.avgDamage,
                avgCs = result.avgCs,
                avgGold = result.avgGold,
                avgVisionScore = result.avgVisionScore,
                elo = result.elo,
                eloRank = result.eloRank,
                eloRankedTotal = result.eloRankedTotal,
                rating = result.rating?.let(EloRankEntryResponse::from),
            )
    }
}

@Schema(name = "SummonerStreak")
data class SummonerStreakResponse(
    @field:Schema(description = "양수 = 연승, 음수 = 연패, 0 = 경기 없음")
    val current: Int,
    @field:Schema(description = "WIN / LOSS / NONE")
    val type: String,
    val longestWin: Int,
    val longestLoss: Int,
    @field:Schema(description = "최근 10경기 결과, 최신순. \"W\" / \"L\"")
    val recentForm: List<String>,
) {
    companion object {
        fun from(result: SummonerStreak) =
            SummonerStreakResponse(
                current = result.current,
                type = result.type,
                longestWin = result.longestWin,
                longestLoss = result.longestLoss,
                recentForm = result.recentForm,
            )
    }
}

@Schema(name = "SummonerTeammate")
data class SummonerTeammateResponse(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val adjustedWinRate: Double,
    val sampleGrade: String,
) {
    companion object {
        fun from(result: SummonerTeammate) =
            SummonerTeammateResponse(
                riotId = result.riotId,
                games = result.games,
                wins = result.wins,
                winRate = result.winRate,
                adjustedWinRate = result.adjustedWinRate,
                sampleGrade = result.sampleGrade,
            )
    }
}

@Schema(name = "SummonerOpponent")
data class SummonerOpponentResponse(
    val riotId: String,
    val games: Int,
    @field:Schema(description = "이 소환사가 이긴 횟수")
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val adjustedWinRate: Double,
    val sampleGrade: String,
) {
    companion object {
        fun from(result: SummonerOpponent) =
            SummonerOpponentResponse(
                riotId = result.riotId,
                games = result.games,
                wins = result.wins,
                losses = result.losses,
                winRate = result.winRate,
                adjustedWinRate = result.adjustedWinRate,
                sampleGrade = result.sampleGrade,
            )
    }
}
