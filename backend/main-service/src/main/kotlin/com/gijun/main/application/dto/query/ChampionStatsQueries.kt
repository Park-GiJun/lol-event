package com.gijun.main.application.dto.query

import com.gijun.main.domain.match.enums.GameMode

data class GetChampionStatsQuery(
    val champion: String,
    val mode: GameMode,
)

/** 둘 다 주면 그 맞대결 하나, [champion] 만 주면 그 챔피언의 상대 전부, 둘 다 없으면 전체다. */
data class GetChampionMatchupQuery(
    val champion: String?,
    val vsChampion: String?,
    val mode: GameMode,
)

data class GetChampionCertificateQuery(
    val mode: GameMode,
    val minGames: Int = 3,
)

data class GetChampionTierQuery(
    val mode: GameMode,
    val minGames: Int = 3,
)
