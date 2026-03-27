package com.gijun.main.application.dto.stats.result

data class KillParticipationEntry(
    val riotId: String,
    val games: Int,
    val avgKp: Double,      // (kills+assists) / teamKills
    val avgKpWin: Double,
    val avgKpLoss: Double,
    val avgKills: Double,
    val avgAssists: Double,
)

data class KillParticipationResult(
    val rankings: List<KillParticipationEntry>,
    val kpKing: String?,
)
