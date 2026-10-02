package com.gijun.main.application.dto.query

import com.gijun.main.domain.match.enums.GameMode

data class GetSessionDetailQuery(
    /** `yyyy-MM-dd`. 세션은 오전 6 시에 시작하는 하루다. */
    val date: String,
    val mode: GameMode,
)
