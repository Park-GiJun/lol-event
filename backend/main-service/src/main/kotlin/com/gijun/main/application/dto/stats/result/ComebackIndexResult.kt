package com.gijun.main.application.dto.stats.result

data class ComebackIndexEntry(
    val riotId: String,
    val totalGames: Int,
    val totalWinRate: Int,
    val contestGames: Int,
    val contestWinRate: Int,
    val surrenderGames: Int,
    val surrenderWinRate: Int,
    val comebackBonus: Int,
    val isKing: Boolean,
)

data class ComebackMatchEntry(
    val matchId: String,
    val gameCreation: Long,
    val gameDurationMin: Double,
    val winnerParticipants: List<String>,
)

data class ComebackIndexResult(
    val rankings: List<ComebackIndexEntry>,
    val comebackKing: String?,
    val topComebackMatches: List<ComebackMatchEntry>,
)
