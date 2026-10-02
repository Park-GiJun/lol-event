package com.gijun.main.infrastructure.adapter.`in`.web.rating.dto

import com.gijun.main.application.dto.result.EloHistoryEntry
import com.gijun.main.application.dto.result.EloLeaderboardResult
import com.gijun.main.application.dto.result.EloRankEntry
import com.gijun.main.application.dto.result.PlayerEloHistoryResult
import com.gijun.main.application.dto.result.PlayerRatingResult
import com.gijun.main.application.dto.result.PredictionMetrics
import com.gijun.main.application.dto.result.RatingValidationResult
import com.gijun.main.application.dto.result.RecalculateResult
import com.gijun.main.application.dto.result.SplitHalfReliability
import com.gijun.main.application.dto.result.ValidationScope
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "EloRankEntry")
data class EloRankEntryResponse(
    @field:Schema(description = "배치 중인 플레이어는 0. 순위를 매기지 않는다.")
    val rank: Int,
    val riotId: String,
    @field:Schema(description = "실력 레이팅 원값.")
    val laneElo: Double,
    @field:Schema(description = "실력 레이팅 표시값(수축 적용). 정렬·표시는 이 값 기준이다.")
    val laneEloDisplay: Double,
    val laneDuels: Int,
    val laneWins: Int,
    val laneLosses: Int,
    @field:Schema(description = "라인 맞대결 승률(0~1).")
    val laneWinRate: Double,
    @field:Schema(description = "전적 레이팅 원값.")
    val teamElo: Double,
    val teamEloDisplay: Double,
    val teamGames: Int,
    @field:Schema(description = "팀 승률(0~1).")
    val winRate: Double,
    @field:Schema(description = "`teamElo - laneElo`. 절댓값이 크면 편성자의 평가와 라인 실적이 어긋나 있다는 뜻이라 팀을 짤 때 참고한다. 레이팅 계산에는 쓰지 않는다.")
    val gap: Double,
    @field:Schema(description = "라인 맞대결 표본 미달. 목록에는 남기되 순위에서는 뺀다.")
    val placement: Boolean,
    @field:Schema(description = "가장 많이 뛴 포지션. 기록이 없으면 null.")
    val mainPosition: String?,
    @field:Schema(description = "*여기부터 화면 호환용 필드다.** `laneElo` 와 같은 값. 화면이 \"Elo\" 라고 부르던 자리가 이제 라인 레이팅이다.")
    val elo: Double,
    @field:Schema(description = "`teamGames` 와 같은 값.")
    val games: Int,
    val wins: Int,
    val losses: Int,
    @field:Schema(description = "연승/연패. 표시 전용이며 레이팅 산식에는 쓰이지 않는다.")
    val winStreak: Int,
    val lossStreak: Int,
    @field:Schema(description = "HIGH / MEDIUM / LOW / INSUFFICIENT — 라인 맞대결 수 기준.")
    val sampleGrade: String,
) {
    companion object {
        fun from(result: EloRankEntry) =
            EloRankEntryResponse(
                rank = result.rank,
                riotId = result.riotId,
                laneElo = result.laneElo,
                laneEloDisplay = result.laneEloDisplay,
                laneDuels = result.laneDuels,
                laneWins = result.laneWins,
                laneLosses = result.laneLosses,
                laneWinRate = result.laneWinRate,
                teamElo = result.teamElo,
                teamEloDisplay = result.teamEloDisplay,
                teamGames = result.teamGames,
                winRate = result.winRate,
                gap = result.gap,
                placement = result.placement,
                mainPosition = result.mainPosition,
                elo = result.elo,
                games = result.games,
                wins = result.wins,
                losses = result.losses,
                winStreak = result.winStreak,
                lossStreak = result.lossStreak,
                sampleGrade = result.sampleGrade,
            )
    }
}

@Schema(name = "EloLeaderboardResult")
data class EloLeaderboardResponse(
    @field:Schema(description = "순위가 매겨진 플레이어가 먼저, 배치 중인 플레이어가 뒤에 온다.")
    val players: List<EloRankEntryResponse>,
    @field:Schema(description = "순위에 들어가기 위해 필요한 최소 라인 맞대결 수.")
    val minDuels: Int,
    val rankedCount: Int,
    val placementCount: Int,
) {
    companion object {
        fun from(result: EloLeaderboardResult) =
            EloLeaderboardResponse(
                players = result.players.map(EloRankEntryResponse::from),
                minDuels = result.minDuels,
                rankedCount = result.rankedCount,
                placementCount = result.placementCount,
            )
    }
}

@Schema(name = "EloHistoryEntry")
data class EloHistoryEntryResponse(
    val matchId: String,
    val eloBefore: Double,
    val eloAfter: Double,
    val delta: Double,
    val win: Boolean,
    @field:Schema(description = "라인 맞대결 결과. WIN / LOSS / NONE(대결 불성립 — 칼바람이거나 포지션이 깨진 경기).")
    val laneResult: String,
    @field:Schema(description = "라인 상대. 대결이 없었으면 null.")
    val laneOpponent: String?,
    val teamEloBefore: Double,
    val teamEloAfter: Double,
    val teamDelta: Double,
    val gameCreation: Long,
) {
    companion object {
        fun from(result: EloHistoryEntry) =
            EloHistoryEntryResponse(
                matchId = result.matchId,
                eloBefore = result.eloBefore,
                eloAfter = result.eloAfter,
                delta = result.delta,
                win = result.win,
                laneResult = result.laneResult,
                laneOpponent = result.laneOpponent,
                teamEloBefore = result.teamEloBefore,
                teamEloAfter = result.teamEloAfter,
                teamDelta = result.teamDelta,
                gameCreation = result.gameCreation,
            )
    }
}

@Schema(name = "PlayerEloHistoryResult")
data class PlayerEloHistoryResponse(
    val riotId: String,
    @field:Schema(description = "라인 레이팅 원값.")
    val currentElo: Double,
    val eloRank: Int?,
    val history: List<EloHistoryEntryResponse>,
) {
    companion object {
        fun from(result: PlayerEloHistoryResult) =
            PlayerEloHistoryResponse(
                riotId = result.riotId,
                currentElo = result.currentElo,
                eloRank = result.eloRank,
                history = result.history.map(EloHistoryEntryResponse::from),
            )
    }
}

@Schema(name = "RatingValidationResult")
data class RatingValidationResponse(
    val totalMatches: Int,
    @field:Schema(description = "재생 대상 필터(10명 · 600초 초과)를 통과한 경기.")
    val ratableMatches: Int,
    @field:Schema(description = "예열에 쓴 경기 수. 이 경기들은 레이팅을 움직이지만 평가에는 들어가지 않는다.")
    val warmup: Int,
    @field:Schema(description = "6시간 이상 간격으로 끊어 센 세션 수.")
    val sessions: Int,
    @field:Schema(description = "직전 경기와 팀 구성이 같아 평가에서 뺀 경기 수.")
    val excludedRepeatedTeams: Int,
    @field:Schema(description = "LaneMethod 이름 -> 경기 수. 방법이 섞여 있으면 여기서 보인다.")
    val methodCounts: Map<String, Int>,
    @field:Schema(description = "평가 대상 전체.")
    val overall: ValidationScopeResponse,
    @field:Schema(description = "세션의 첫 경기만. **이쪽이 더 정직한 숫자다** — 같은 세션 안에서는 팀 구성이 반복되기 쉬워 \"직전 승자가 또 이긴다\"는 누수가 섞인다.")
    val sessionFirst: ValidationScopeResponse,
    val splitHalf: SplitHalfReliabilityResponse,
) {
    companion object {
        fun from(result: RatingValidationResult) =
            RatingValidationResponse(
                totalMatches = result.totalMatches,
                ratableMatches = result.ratableMatches,
                warmup = result.warmup,
                sessions = result.sessions,
                excludedRepeatedTeams = result.excludedRepeatedTeams,
                methodCounts = result.methodCounts,
                overall = ValidationScopeResponse.from(result.overall),
                sessionFirst = ValidationScopeResponse.from(result.sessionFirst),
                splitHalf = SplitHalfReliabilityResponse.from(result.splitHalf),
            )
    }
}

@Schema(name = "ValidationScope")
data class ValidationScopeResponse(
    val games: Int,
    @field:Schema(description = "무정보 기준선(항상 0.5). 로그로스 ln2 = 0.6931. 이보다 나쁘면 그 예측기는 해롭다.")
    val baseline: PredictionMetricsResponse,
    @field:Schema(description = "라인 레이팅 팀 평균으로 승패를 예측.")
    val laneElo: PredictionMetricsResponse,
    @field:Schema(description = "전적 레이팅 팀 평균으로 승패를 예측.")
    val teamElo: PredictionMetricsResponse,
) {
    companion object {
        fun from(result: ValidationScope) =
            ValidationScopeResponse(
                games = result.games,
                baseline = PredictionMetricsResponse.from(result.baseline),
                laneElo = PredictionMetricsResponse.from(result.laneElo),
                teamElo = PredictionMetricsResponse.from(result.teamElo),
            )
    }
}

@Schema(name = "PredictionMetrics")
data class PredictionMetricsResponse(
    val logLoss: Double,
    val accuracy: Double,
) {
    companion object {
        fun from(result: PredictionMetrics) =
            PredictionMetricsResponse(
                logLoss = result.logLoss,
                accuracy = result.accuracy,
            )
    }
}

@Schema(name = "SplitHalfReliability")
data class SplitHalfReliabilityResponse(
    @field:Schema(description = "양쪽 반에 [minDuelsPerHalf] 이상을 가진 사람 수.")
    val players: Int,
    val minDuelsPerHalf: Int,
    @field:Schema(description = "두 반쪽 승률의 피어슨 상관.")
    val correlation: Double,
    @field:Schema(description = "스피어만-브라운 보정값 `2r/(1+r)`. 전체 길이 기준으로 환산한 신뢰도.")
    val spearmanBrown: Double,
) {
    companion object {
        fun from(result: SplitHalfReliability) =
            SplitHalfReliabilityResponse(
                players = result.players,
                minDuelsPerHalf = result.minDuelsPerHalf,
                correlation = result.correlation,
                spearmanBrown = result.spearmanBrown,
            )
    }
}

@Schema(name = "RecalculateResult")
data class RecalculateResponse(
    val totalMatches: Int,
    val ratedMatches: Int,
    val players: Int,
    val laneDuels: Int,
    @field:Schema(description = "LaneMethod 이름 -> 그 방법으로 판정한 경기 수.")
    val methodCounts: Map<String, Int>,
) {
    companion object {
        fun from(result: RecalculateResult) =
            RecalculateResponse(
                totalMatches = result.totalMatches,
                ratedMatches = result.ratedMatches,
                players = result.players,
                laneDuels = result.laneDuels,
                methodCounts = result.methodCounts,
            )
    }
}

@Schema(name = "PlayerRatingResult")
data class PlayerRatingResponse(
    val id: Long,
    val riotId: String,
    val laneElo: Double,
    val laneDuels: Int,
    val laneWins: Int,
    val teamElo: Double,
    val teamGames: Int,
    val teamWins: Int,
    val teamWinStreak: Int,
    val teamLossStreak: Int,
    val updatedAt: LocalDateTime,
    @field:Schema(description = "배치 중. 순위에서 뺀다.")
    val placement: Boolean,
    val laneEloDisplay: Double,
    val teamEloDisplay: Double,
    @field:Schema(description = "`teamElo - laneElo`. 편성자의 평가와 라인 실적이 얼마나 어긋나 있는지.")
    val gap: Double,
) {
    companion object {
        fun from(result: PlayerRatingResult) =
            PlayerRatingResponse(
                id = result.id,
                riotId = result.riotId,
                laneElo = result.laneElo,
                laneDuels = result.laneDuels,
                laneWins = result.laneWins,
                teamElo = result.teamElo,
                teamGames = result.teamGames,
                teamWins = result.teamWins,
                teamWinStreak = result.teamWinStreak,
                teamLossStreak = result.teamLossStreak,
                updatedAt = result.updatedAt,
                placement = result.placement,
                laneEloDisplay = result.laneEloDisplay,
                teamEloDisplay = result.teamEloDisplay,
                gap = result.gap,
            )
    }
}
