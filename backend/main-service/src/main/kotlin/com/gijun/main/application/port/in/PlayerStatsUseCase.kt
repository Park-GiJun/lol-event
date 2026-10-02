package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.DuoStatsResult
import com.gijun.main.application.dto.result.GrowthCurveResult
import com.gijun.main.application.dto.result.MvpStatsResult
import com.gijun.main.application.dto.result.OverviewStats
import com.gijun.main.application.dto.result.PlayerComparisonResult
import com.gijun.main.application.dto.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.result.PlaystyleDnaResult
import com.gijun.main.application.dto.result.StatsResult
import com.gijun.main.application.dto.result.StreakResult

interface GetStatsUseCase {
    fun getStats(mode: String): StatsResult
}

interface GetPlayerStatsUseCase {
    fun getPlayerStats(
        riotId: String,
        mode: String,
        lane: String? = null,
    ): PlayerDetailStatsResult
}

interface GetOverviewStatsUseCase {
    fun getOverviewStats(mode: String): OverviewStats
}

interface GetDuoStatsUseCase {
    fun getDuoStats(
        mode: String,
        minGames: Int,
    ): DuoStatsResult
}

interface GetPlayerStreakUseCase {
    fun getPlayerStreak(
        riotId: String,
        mode: String,
    ): StreakResult
}

interface GetMvpStatsUseCase {
    fun getMvpStats(mode: String): MvpStatsResult
}

interface GetGrowthCurveUseCase {
    fun getGrowthCurve(
        riotId: String,
        mode: String,
    ): GrowthCurveResult
}

interface GetPlaystyleDnaUseCase {
    fun getPlaystyleDna(mode: String): PlaystyleDnaResult
}

interface GetPlayerComparisonUseCase {
    fun getPlayerComparison(
        player1: String,
        player2: String,
        mode: String,
    ): PlayerComparisonResult
}
