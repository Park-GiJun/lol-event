package com.gijun.main.application.dto.result

import com.gijun.main.domain.rating.model.PlayerRatingModel
import java.time.LocalDateTime

/**
 * 리더보드 한 줄. **정렬 기본값은 `laneEloDisplay`** 다 — 실력 레이팅이 이 표의 주인공이고,
 * 표시값은 표본이 적은 사람이 과하게 튀지 않도록 수축한 값이다.
 *
 * 원값과 표시값을 둘 다 내려준다. 화면은 표시값을 쓰고, 편성 보조처럼 **계산에 쓰는 쪽은 원값**을
 * 써야 한다 — 수축값을 다시 계산에 넣으면 검증에서 더 나빴다.
 */
data class EloRankEntry(
    /** 배치 중인 플레이어는 0. 순위를 매기지 않는다. */
    val rank: Int,
    val riotId: String,
    /** 실력 레이팅 원값. */
    val laneElo: Double,
    /** 실력 레이팅 표시값(수축 적용). 정렬·표시는 이 값 기준이다. */
    val laneEloDisplay: Double,
    val laneDuels: Int,
    val laneWins: Int,
    val laneLosses: Int,
    /** 라인 맞대결 승률(0~1). */
    val laneWinRate: Double,
    /** 전적 레이팅 원값. */
    val teamElo: Double,
    val teamEloDisplay: Double,
    val teamGames: Int,
    /** 팀 승률(0~1). */
    val winRate: Double,
    /**
     * `teamElo - laneElo`. 절댓값이 크면 편성자의 평가와 라인 실적이 어긋나 있다는 뜻이라
     * 팀을 짤 때 참고한다. 레이팅 계산에는 쓰지 않는다.
     */
    val gap: Double,
    /** 라인 맞대결 표본 미달. 목록에는 남기되 순위에서는 뺀다. */
    val placement: Boolean,
    /** 가장 많이 뛴 포지션. 기록이 없으면 null. */
    val mainPosition: String?,
    /** **여기부터 화면 호환용 필드다.** `laneElo` 와 같은 값. 화면이 "Elo" 라고 부르던 자리가 이제 라인 레이팅이다. */
    val elo: Double,
    /** `teamGames` 와 같은 값. */
    val games: Int,
    val wins: Int,
    val losses: Int,
    /** 연승/연패. 표시 전용이며 레이팅 산식에는 쓰이지 않는다. */
    val winStreak: Int,
    val lossStreak: Int,
    /** HIGH / MEDIUM / LOW / INSUFFICIENT — 라인 맞대결 수 기준. */
    val sampleGrade: String,
)

data class EloLeaderboardResult(
    /** 순위가 매겨진 플레이어가 먼저, 배치 중인 플레이어가 뒤에 온다. */
    val players: List<EloRankEntry>,
    /** 순위에 들어가기 위해 필요한 최소 라인 맞대결 수. */
    val minDuels: Int,
    val rankedCount: Int,
    val placementCount: Int,
)

/**
 * 한 경기가 두 레이팅을 어떻게 움직였는지.
 *
 * `elo*` 는 **라인 레이팅**이다. 화면이 "Elo" 라고 부르는 값이 이제 laneElo 라서,
 * 필드 이름을 바꾸지 않고 뜻만 옮겼다. 팀 레이팅은 `team*` 로 따로 싣는다.
 */
data class EloHistoryEntry(
    val matchId: String,
    val eloBefore: Double,
    val eloAfter: Double,
    val delta: Double,
    val win: Boolean,
    /** 라인 맞대결 결과. WIN / LOSS / NONE(대결 불성립 — 칼바람이거나 포지션이 깨진 경기). */
    val laneResult: String,
    /** 라인 상대. 대결이 없었으면 null. */
    val laneOpponent: String?,
    val teamEloBefore: Double,
    val teamEloAfter: Double,
    val teamDelta: Double,
    val gameCreation: Long,
)

data class PlayerEloHistoryResult(
    val riotId: String,
    /** 라인 레이팅 원값. */
    val currentElo: Double,
    val eloRank: Int?,
    val history: List<EloHistoryEntry>,
)

/**
 * 워크포워드 검증 결과.
 *
 * **이 내전은 사전 데이터로 승패를 잘 맞힐 수 없는 구조다.** 매 세션 한 사람이 포지션까지 보고
 * 팀을 짜기 때문에, 편성자가 균형을 맞춘 만큼 남는 건 실력차가 아니라 편성자의 추정 오차다.
 * 숫자가 낮다고 버그로 의심하지 마라. 기대치는 아래와 같다.
 *
 * - 라인 Elo 의 팀 승패 적중률: 약 63% (세션 첫 경기 기준)
 * - 같은 팀 분할이 반복된 21쌍에서 같은 팀이 재승리한 비율 66.7% → 팀 구성을 완벽히 알아도
 *   이론적 상한이 약 79%, 현실적으로 65~70%
 * - 특징 28개로 로지스틱·GBM·랜덤포레스트를 돌려도 전부 무정보 기준선보다 로그로스가 나빴다
 */
data class RatingValidationResult(
    val totalMatches: Int,
    /** 재생 대상 필터(10명 · 600초 초과)를 통과한 경기. */
    val ratableMatches: Int,
    /** 예열에 쓴 경기 수. 이 경기들은 레이팅을 움직이지만 평가에는 들어가지 않는다. */
    val warmup: Int,
    /** 6시간 이상 간격으로 끊어 센 세션 수. */
    val sessions: Int,
    /** 직전 경기와 팀 구성이 같아 평가에서 뺀 경기 수. */
    val excludedRepeatedTeams: Int,
    /** LaneMethod 이름 -> 경기 수. 방법이 섞여 있으면 여기서 보인다. */
    val methodCounts: Map<String, Int>,
    /** 평가 대상 전체. */
    val overall: ValidationScope,
    /**
     * 세션의 첫 경기만. **이쪽이 더 정직한 숫자다** — 같은 세션 안에서는 팀 구성이 반복되기 쉬워
     * "직전 승자가 또 이긴다"는 누수가 섞인다.
     */
    val sessionFirst: ValidationScope,
    val splitHalf: SplitHalfReliability,
)

/** 라인 맞대결 예측. 값은 전부 로그로스·적중률이고 기준선은 0.6931 / 0.5 다. */
data class LaneDuelValidation(
    val duels: Int,
    val baseline: PredictionMetrics,
    /** 두 사람의 전체 라인 Elo 로 예측. */
    val laneElo: PredictionMetrics,
    /** 두 사람의 자리 Elo 로 예측. */
    val seatElo: PredictionMetrics,
    /**
     * 수축의 사전 표본 수를 바꿔 가며 잰 자리 Elo. 키가 그 값이다.
     * 적중률은 오르는데 로그로스가 나쁘면 방향은 맞고 **폭이 과한** 것이다 — 그때 더 큰 값을 고른다.
     */
    val seatEloByShrink: Map<Int, PredictionMetrics>,
)

/** 한 평가 구간에서 예측기들을 나란히 비교한다. */
data class ValidationScope(
    val games: Int,
    /** 무정보 기준선(항상 0.5). 로그로스 ln2 = 0.6931. 이보다 나쁘면 그 예측기는 해롭다. */
    val baseline: PredictionMetrics,
    /** 라인 레이팅 팀 평균으로 승패를 예측. */
    val laneElo: PredictionMetrics,
    /** 전적 레이팅 팀 평균으로 승패를 예측. */
    val teamElo: PredictionMetrics,
    /** 자리 Elo(그 포지션에서의 라인 실적을 반영한 라인 Elo) 팀 평균으로 승패를 예측. */
    val seatElo: PredictionMetrics,
    /** 수축의 사전 표본 수별 자리 Elo 팀 평균 예측. 키가 그 값이다. */
    val seatEloByShrink: Map<Int, PredictionMetrics>,
    /**
     * 라인 맞대결 하나하나를 맞히는 검증. 팀 승패는 편성자가 이미 균형을 맞춘 결과라 둔하다 —
     * 자리 Elo 가 쓸모 있는지는 **이쪽**에서 [LaneDuelValidation.seatElo] 가 [LaneDuelValidation.laneElo] 보다 낮은지로 본다.
     */
    val laneDuels: LaneDuelValidation,
)

data class PredictionMetrics(
    val logLoss: Double,
    val accuracy: Double,
)

/**
 * 반분 신뢰도 — 같은 사람의 라인 맞대결을 홀수/짝수로 반 갈라 각각 승률을 내고, 그 둘이
 * 사람들 사이에서 얼마나 같이 움직이는지 본다. 측정하고 있는 것이 실력이라면 두 반쪽이
 * 같은 방향을 가리켜야 한다.
 *
 * 현재 LEGACY_FINAL 기준 0.568. 타임라인 경기가 40개쯤 쌓였을 때 이 값이 오르면 확정 개선이다.
 * K 가 커질수록 이 값은 단조 감소한다 (K=8 에서 0.629 → K=64 에서 0.470).
 */
data class SplitHalfReliability(
    /** 양쪽 반에 [minDuelsPerHalf] 이상을 가진 사람 수. */
    val players: Int,
    val minDuelsPerHalf: Int,
    /** 두 반쪽 승률의 피어슨 상관. */
    val correlation: Double,
    /** 스피어만-브라운 보정값 `2r/(1+r)`. 전체 길이 기준으로 환산한 신뢰도. */
    val spearmanBrown: Double,
)

/**
 * 전체 재집계 결과 요약. 재집계가 실제로 무엇을 셌는지 눈으로 확인하기 위한 값이다.
 *
 * [ratedMatches] 가 기대보다 적으면 재생 대상 필터(10명 · 600초 초과)에 걸린 경기가 있다는 뜻이고,
 * [laneDuels] 가 `ratedMatches × 5` 에 못 미치면 포지션 배정이 깨진 팀이 있다는 뜻이다.
 */
data class RecalculateResult(
    val totalMatches: Int,
    val ratedMatches: Int,
    val players: Int,
    val laneDuels: Int,
    /** LaneMethod 이름 -> 그 방법으로 판정한 경기 수. */
    val methodCounts: Map<String, Int>,
)

/**
 * 한 사람의 두 레이팅 원값과, 거기서 파생한 표시값.
 *
 * 예전에는 관리자 API 가 도메인 모델([PlayerRatingModel])을 그대로 내보냈다. 모델의 프로퍼티
 * 이름이 곧 응답 스키마라, 모델을 손보면 화면이 조용히 깨진다. 필드는 그때와 같다.
 */
data class PlayerRatingResult(
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
    /** 배치 중. 순위에서 뺀다. */
    val placement: Boolean,
    val laneEloDisplay: Double,
    val teamEloDisplay: Double,
    /** `teamElo - laneElo`. 편성자의 평가와 라인 실적이 얼마나 어긋나 있는지. */
    val gap: Double,
) {
    companion object {
        fun from(model: PlayerRatingModel) =
            PlayerRatingResult(
                id = model.id,
                riotId = model.riotId,
                laneElo = model.laneElo,
                laneDuels = model.laneDuels,
                laneWins = model.laneWins,
                teamElo = model.teamElo,
                teamGames = model.teamGames,
                teamWins = model.teamWins,
                teamWinStreak = model.teamWinStreak,
                teamLossStreak = model.teamLossStreak,
                updatedAt = model.updatedAt,
                placement = model.placement,
                laneEloDisplay = model.laneEloDisplay,
                teamEloDisplay = model.teamEloDisplay,
                gap = model.gap,
            )
    }
}
