package com.gijun.main.application.dto.stats.result

/**
 * 세션 하나. 세션은 **오전 6시에 시작하는 하루**다 — 경계와 이유는
 * [com.gijun.main.domain.service.SessionClock] 에 있다.
 */
data class SessionEntry(
    /** `yyyy-MM-dd`. 세션 상세(`/api/stats/sessions/{date}`)의 식별자다. */
    val date: String,
    /** 그 세션의 경기들. 시작 시각 오름차순. 상세로 들어가려면 이게 필요하다. */
    val matchIds: List<String>,
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
