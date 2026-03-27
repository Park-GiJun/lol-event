package com.gijun.main.application.dto.stats.result

data class DayPatternEntry(
    val dayOfWeek: Int,   // 1=Monday..7=Sunday (ISO)
    val dayName: String,  // "월", "화", ...
    val sessions: Int,
    val games: Int,
    val winRate: Double,
)

data class HourPatternEntry(
    val hour: Int,
    val games: Int,
    val winRate: Double,
)

data class TimePatternResult(
    val byDay: List<DayPatternEntry>,
    val byHour: List<HourPatternEntry>,
    val busiestDay: String?,
    val busiestHour: Int?,
    val totalGames: Int,
)
