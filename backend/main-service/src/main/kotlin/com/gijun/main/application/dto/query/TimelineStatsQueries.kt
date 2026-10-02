package com.gijun.main.application.dto.query

import com.gijun.main.domain.match.enums.GameMode

data class GetTimelineLaneQuery(
    /** TOP / JUNGLE / MID / ADC / SUPPORT. */
    val lane: String,
    val mode: GameMode,
)

data class GetTimelineChampionsQuery(
    val mode: GameMode,
    /** 주면 그 챔피언만 (영문명, 대소문자 무시). */
    val champion: String?,
)
