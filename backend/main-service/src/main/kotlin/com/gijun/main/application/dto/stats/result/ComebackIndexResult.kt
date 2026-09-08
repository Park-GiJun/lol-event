package com.gijun.main.application.dto.stats.result

/**
 * 역전 지표.
 *
 * surrenderGames / surrenderWinRate 는 걷어냈다. 조기 항복 경기를 세던 값인데
 * game_ended_in_early_surrender 가 전 행 false 라 항상 0 이었다.
 */
data class ComebackIndexEntry(
    val riotId: String,
    val totalGames: Int,
    val totalWinRate: Int,
    val contestGames: Int,
    val contestWinRate: Int,
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
