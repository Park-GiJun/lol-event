package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetTimelineChampionsQuery
import com.gijun.main.application.dto.query.GetTimelineLaneQuery
import com.gijun.main.application.dto.result.PlayerTimelineResult
import com.gijun.main.application.dto.result.TimelineChampionsResult
import com.gijun.main.application.dto.result.TimelineLaneResult
import com.gijun.main.application.dto.result.TimelineStatsResult
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.domain.vo.RiotId

interface GetTimelineStatsUseCase {
    fun getTimelineStats(mode: GameMode): TimelineStatsResult
}

interface GetTimelineLaneUseCase {
    fun getTimelineLane(query: GetTimelineLaneQuery): TimelineLaneResult
}

interface GetTimelineChampionsUseCase {
    fun getTimelineChampions(query: GetTimelineChampionsQuery): TimelineChampionsResult
}

interface GetPlayerTimelineUseCase {
    fun getPlayerTimeline(riotId: RiotId): PlayerTimelineResult
}
