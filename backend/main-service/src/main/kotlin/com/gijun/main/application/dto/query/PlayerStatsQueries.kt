package com.gijun.main.application.dto.query

import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.domain.vo.RiotId

data class GetPlayerStatsQuery(
    val riotId: RiotId,
    val mode: GameMode,
    /** TOP / JUNGLE / MID / BOTTOM / SUPPORT. null 이면 전 포지션. */
    val lane: String? = null,
)

data class GetPlayerStreakQuery(
    val riotId: RiotId,
    val mode: GameMode,
)

data class GetGrowthCurveQuery(
    val riotId: RiotId,
    val mode: GameMode,
)

data class GetPlayerComparisonQuery(
    val player1: RiotId,
    val player2: RiotId,
    val mode: GameMode,
)
