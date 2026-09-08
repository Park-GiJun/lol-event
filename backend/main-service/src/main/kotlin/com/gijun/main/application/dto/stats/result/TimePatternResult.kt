package com.gijun.main.application.dto.stats.result

/**
 * 요일·시간대별 경기 수.
 *
 * 예전에는 winRate 도 함께 내보냈는데 양쪽 다 값이 성립하지 않았다.
 * 요일 쪽은 "이긴 팀이 하나라도 있으면 1승"으로 세고 있어서 늘 100% 근처(87~100%)였고,
 * 시간 쪽은 계산 자체를 안 하고 0.0 을 박아 내보내고 있었다.
 *
 * 애초에 양 팀이 모두 우리 쪽인 내전에서는 경기 단위 승률이라는 게 정의되지 않는다.
 * 화면도 games 만 그리고 있었으므로 필드를 걷어냈다.
 */
data class DayPatternEntry(
    val dayOfWeek: Int,   // 1=Monday..7=Sunday (ISO)
    val dayName: String,  // "월", "화", ...
    val sessions: Int,
    val games: Int,
)

data class HourPatternEntry(
    val hour: Int,
    val games: Int,
)

data class TimePatternResult(
    val byDay: List<DayPatternEntry>,
    val byHour: List<HourPatternEntry>,
    val busiestDay: String?,
    val busiestHour: Int?,
    val totalGames: Int,
)
