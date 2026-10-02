package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetGrowthCurveQuery
import com.gijun.main.application.dto.query.GetPlayerComparisonQuery
import com.gijun.main.application.dto.query.GetPlayerStatsQuery
import com.gijun.main.application.dto.query.GetPlayerStreakQuery
import com.gijun.main.application.dto.result.GrowthCurveResult
import com.gijun.main.application.dto.result.PlayerComparisonResult
import com.gijun.main.application.dto.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.result.StatsResult
import com.gijun.main.application.dto.result.StreakResult
import com.gijun.main.domain.match.enums.GameMode

interface GetStatsUseCase {
    fun getStats(mode: GameMode): StatsResult
}

interface GetPlayerStatsUseCase {
    fun getPlayerStats(query: GetPlayerStatsQuery): PlayerDetailStatsResult
}

interface GetPlayerStreakUseCase {
    fun getPlayerStreak(query: GetPlayerStreakQuery): StreakResult
}

interface GetGrowthCurveUseCase {
    fun getGrowthCurve(query: GetGrowthCurveQuery): GrowthCurveResult
}

interface GetPlayerComparisonUseCase {
    fun getPlayerComparison(query: GetPlayerComparisonQuery): PlayerComparisonResult
}
