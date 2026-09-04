package com.gijun.main.application.dto.stats.result

data class EloHistoryEntry(
    val matchId: String,
    val eloBefore: Double,
    val eloAfter: Double,
    val delta: Double,
    val win: Boolean,
    /** 같은 포지션 상대와 비교한 라인전 점수(0~1, 0.5가 호각). 변동폭 차이의 근거다. */
    val lanePerformance: Double,
    val gameCreation: Long,
)

data class PlayerEloHistoryResult(
    val riotId: String,
    val currentElo: Double,
    val eloRank: Int?,
    val history: List<EloHistoryEntry>,
)
