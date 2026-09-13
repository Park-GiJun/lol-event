package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.stats.result.PlayerTimelineResult
import com.gijun.main.application.dto.stats.result.TimelineStatsResult

interface GetTimelineStatsUseCase {
    fun getTimelineStats(mode: String): TimelineStatsResult
}

interface GetPlayerTimelineUseCase {
    fun getPlayerTimeline(riotId: String): PlayerTimelineResult
}
