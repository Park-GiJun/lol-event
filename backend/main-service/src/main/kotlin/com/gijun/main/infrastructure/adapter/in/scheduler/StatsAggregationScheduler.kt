package com.gijun.main.infrastructure.adapter.`in`.scheduler

import com.gijun.main.application.port.`in`.TriggerStatsAggregationUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StatsAggregationScheduler(
    private val triggerStatsAggregationUseCase: TriggerStatsAggregationUseCase,
) {
    /** 매일 새벽 4시 정기 집계 */
    @Scheduled(cron = "0 0 4 * * *")
    fun scheduledAggregation() {
        triggerStatsAggregationUseCase.triggerStatsAggregation(REASON)
    }

    private companion object {
        private const val REASON = "scheduled"
    }
}
