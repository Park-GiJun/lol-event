package com.gijun.main.application.dto.query

import com.gijun.main.shared.domain.vo.RiotId

data class GetEloHistoryQuery(
    val riotId: RiotId,
    val limit: Int = 30,
)

data class ValidateRatingQuery(
    /** 예열 경기 수. 이 경기들은 레이팅을 움직이지만 평가에는 들어가지 않는다. */
    val warmup: Int,
    /** 직전 경기와 팀 구성이 같은(진영만 바뀐 경우 포함) 경기를 평가에서 뺀다. */
    val excludeRepeatedTeams: Boolean,
)
