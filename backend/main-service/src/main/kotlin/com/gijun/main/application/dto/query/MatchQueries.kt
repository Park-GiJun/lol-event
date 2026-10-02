package com.gijun.main.application.dto.query

import com.gijun.main.domain.match.enums.GameMode

data class GetMatchPageQuery(
    val mode: GameMode,
    /** 0 부터 시작한다. */
    val page: Int,
    val size: Int,
)
