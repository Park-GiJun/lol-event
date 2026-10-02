package com.gijun.main.infrastructure.batch

import com.gijun.main.application.port.out.batch.StatsAggregationJobPort
import com.gijun.main.infrastructure.batch.tasklet.ChampionItemStatsAggregationTasklet
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * 집계 잡 실행기. 예전에는 스케줄러가 이 일을 직접 했고, 웹 어댑터와 Kafka 소비자가 그 스케줄러를
 * 주입받아 불렀다 — 인바운드 어댑터끼리 서로를 부르는 구조였다.
 */
@Component
class StatsAggregationJobAdapter(
    private val jobOperator: JobOperator,
    @Qualifier("statsAggregationJob") private val statsAggregationJob: Job,
    private val championItemStatsAggregationTasklet: ChampionItemStatsAggregationTasklet,
) : StatsAggregationJobPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun launch(reason: String) {
        try {
            val params =
                JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .addString("reason", reason)
                    .toJobParameters()
            val execution = jobOperator.start(statsAggregationJob, params)
            log.info("통계 배치 시작 [$reason] — executionId=${execution.id}")
        } catch (e: Exception) {
            log.error("통계 배치 실행 실패 [$reason]: ${e.message}", e)
        }
    }

    override fun aggregateChampionItemStats() = championItemStatsAggregationTasklet.aggregate()
}
