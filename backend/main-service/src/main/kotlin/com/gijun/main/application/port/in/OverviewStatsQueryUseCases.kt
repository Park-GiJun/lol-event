package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.OverviewStats
import com.gijun.main.application.dto.result.WeeklyAwardsResult
import com.gijun.main.domain.match.enums.GameMode

interface GetOverviewStatsUseCase {
    fun getOverviewStats(mode: GameMode): OverviewStats
}

interface GetWeeklyAwardsUseCase {
    fun getWeeklyAwards(mode: GameMode): WeeklyAwardsResult
}
