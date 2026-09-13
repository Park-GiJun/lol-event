package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.stats.result.PlayerTimelineResult
import com.gijun.main.application.dto.stats.result.TimelineChampionsResult
import com.gijun.main.application.dto.stats.result.TimelineLaneResult
import com.gijun.main.application.dto.stats.result.TimelineStatsResult

interface GetTimelineStatsUseCase {
    fun getTimelineStats(mode: String): TimelineStatsResult

    /** [lane] 은 TOP / JUNGLE / MID / ADC / SUPPORT. */
    fun getTimelineLane(lane: String, mode: String): TimelineLaneResult

    /** [champion] 을 주면 그 챔피언만 (영문명, 대소문자 무시). */
    fun getTimelineChampions(mode: String, champion: String?): TimelineChampionsResult
}

interface GetPlayerTimelineUseCase {
    fun getPlayerTimeline(riotId: String): PlayerTimelineResult
}
