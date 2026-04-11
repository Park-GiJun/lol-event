package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.stats.result.DuoStatsResult
import com.gijun.main.application.dto.stats.result.GrowthCurveResult
import com.gijun.main.application.dto.stats.result.MvpStatsResult
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.PlayerComparisonResult
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.PlaystyleDnaResult
import com.gijun.main.application.dto.stats.result.StatsResult
import com.gijun.main.application.dto.stats.result.StreakResult

interface GetStatsUseCase {
    fun getStats(mode: String): StatsResult
}

interface GetPlayerStatsUseCase {
    fun getPlayerStats(riotId: String, mode: String, lane: String? = null): PlayerDetailStatsResult
}

interface GetOverviewStatsUseCase {
    fun getOverviewStats(mode: String): OverviewStats
}

interface GetDuoStatsUseCase {
    fun getDuoStats(mode: String, minGames: Int): DuoStatsResult
}

interface GetPlayerStreakUseCase {
    fun getPlayerStreak(riotId: String, mode: String): StreakResult
}

interface GetMvpStatsUseCase {
    fun getMvpStats(mode: String): MvpStatsResult
}

interface GetGrowthCurveUseCase {
    fun getGrowthCurve(riotId: String, mode: String): GrowthCurveResult
}

interface GetPlaystyleDnaUseCase {
    fun getPlaystyleDna(mode: String): PlaystyleDnaResult
}

interface GetPlayerComparisonUseCase {
    fun getPlayerComparison(player1: String, player2: String, mode: String): PlayerComparisonResult
}
