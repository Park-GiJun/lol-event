package com.gijun.main.application.dto.stats.result

data class StreakResult(
    val riotId: String,
    /** 양수 = 연승, 음수 = 연패, 0 = 경기 없음 */
    val currentStreak: Int,
    /** "WIN", "LOSS", "NONE" */
    val currentStreakType: String,
    val longestWinStreak: Int,
    val longestLossStreak: Int,
    /** 최근 10경기 결과: "W" / "L" (최신순) */
    val recentForm: List<String>,
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
)
