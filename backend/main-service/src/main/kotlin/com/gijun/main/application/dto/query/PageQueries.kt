package com.gijun.main.application.dto.query

import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.domain.vo.RiotId

data class GetChampionPageQuery(
    val champion: String,
    val mode: GameMode,
)

data class GetSummonerProfileQuery(
    val riotId: RiotId,
    val mode: GameMode,
)
