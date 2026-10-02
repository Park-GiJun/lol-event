package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.StatsAggregationStatusResult

interface TriggerStatsAggregationUseCase {
    /** @param reason 로그와 잡 파라미터에 남는 사유(`scheduled` · `kafka-trigger` · `manual-api`). */
    fun triggerStatsAggregation(reason: String)
}

interface AggregateChampionItemStatsUseCase {
    fun aggregateChampionItemStats()
}

interface GetStatsAggregationStatusUseCase {
    fun getStatsAggregationStatus(): StatsAggregationStatusResult
}
