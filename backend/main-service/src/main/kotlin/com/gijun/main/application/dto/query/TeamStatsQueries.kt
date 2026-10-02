package com.gijun.main.application.dto.query

import com.gijun.main.domain.match.enums.GameMode

data class GetDuoStatsQuery(
    val mode: GameMode,
    val minGames: Int,
)

data class GetRivalMatchupQuery(
    val mode: GameMode,
    val minGames: Int = 3,
)
