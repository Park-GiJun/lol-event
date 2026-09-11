package com.gijun.main.application.dto.stats.result

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

    // ── 화면 호환용 ──
    /** `laneElo` 와 같은 값. 화면이 "Elo" 라고 부르던 자리가 이제 라인 레이팅이다. */
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
