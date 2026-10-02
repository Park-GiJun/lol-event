package com.gijun.main.application.dto.query

import com.gijun.main.domain.match.enums.GameMode

data class GetLaneLeaderboardQuery(
    /** TOP / JUNGLE / MID / BOTTOM / SUPPORT. */
    val lane: String,
    val mode: GameMode,
)
