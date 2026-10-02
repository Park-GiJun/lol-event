package com.gijun.main.application.handler

import com.gijun.main.application.port.`in`.AggregateChampionItemStatsUseCase
import com.gijun.main.application.port.`in`.TriggerStatsAggregationUseCase
import com.gijun.main.application.port.out.batch.StatsAggregationJobPort
import org.springframework.stereotype.Service

/**
 * 통계 스냅샷 집계 실행.
 *
 * 집계 자체는 Spring Batch 잡이 한다. 여기는 그 잡을 시작하는 진입점일 뿐이라 트랜잭션이 없다 —
 * 잡이 스텝마다 자기 트랜잭션을 연다.
 */
@Service
class StatsAggregationCommandHandler(
    private val statsAggregationJobPort: StatsAggregationJobPort,
) : TriggerStatsAggregationUseCase,
    AggregateChampionItemStatsUseCase {
    override fun triggerStatsAggregation(reason: String) = statsAggregationJobPort.launch(reason)

    override fun aggregateChampionItemStats() = statsAggregationJobPort.aggregateChampionItemStats()
}
