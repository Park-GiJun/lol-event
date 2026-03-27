package com.gijun.main.application.dto.stats.result

data class SessionEntry(
    val date: String,
    val games: Int,
    val totalDurationMin: Int,
    val sessionMvp: String?,
    val sessionMvpKda: Double,
    val team100Wins: Int,
    val team200Wins: Int,
    val totalKills: Int,
    val pentaKills: Int,
    val participants: List<String>,
)

data class SessionReportResult(
    val sessions: List<SessionEntry>,
    val totalSessions: Int,
)
