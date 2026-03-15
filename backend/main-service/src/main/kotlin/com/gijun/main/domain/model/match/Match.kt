package com.gijun.main.domain.model.match

import java.time.LocalDateTime

data class Match(
    val id: Long = 0,
    val matchId: String,
    val queueId: Int,
    val gameCreation: Long,
    val gameDuration: Int,
    val gameMode: String? = null,
    val gameType: String? = null,
    val gameVersion: String? = null,
    val mapId: Int? = null,
    val seasonId: Int? = null,
    val platformId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val participants: MutableList<MatchParticipant> = mutableListOf(),
    val teams: MutableList<MatchTeam> = mutableListOf()
)
