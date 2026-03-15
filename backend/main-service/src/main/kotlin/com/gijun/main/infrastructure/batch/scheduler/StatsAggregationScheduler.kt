package com.gijun.main.infrastructure.batch.scheduler

import com.gijun.main.infrastructure.batch.config.StatsAggregationJobConfig
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StatsAggregationScheduler(
    private val jobLauncher: JobLauncher,
    @Qualifier("statsAggregationJob") private val statsAggregationJob: Job,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 매일 새벽 4시 정기 집계 */
    @Scheduled(cron = "0 0 4 * * *")
    fun scheduledAggregation() {
        launchJob(reason = "scheduled")
    }

    fun launchJob(reason: String = "manual") {
        try {
            val params = JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .addString("reason", reason)
                .toJobParameters()
            val execution = jobLauncher.run(statsAggregationJob, params)
            log.info("통계 배치 시작 [$reason] — executionId=${execution.id}")
        } catch (e: Exception) {
            log.error("통계 배치 실행 실패 [$reason]: ${e.message}", e)
        }
    }
}
