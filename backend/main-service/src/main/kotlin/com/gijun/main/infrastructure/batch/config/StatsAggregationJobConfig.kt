package com.gijun.main.infrastructure.batch.config

import com.gijun.main.infrastructure.batch.tasklet.ChampionItemStatsAggregationTasklet
import com.gijun.main.infrastructure.batch.tasklet.ChampionStatsAggregationTasklet
import com.gijun.main.infrastructure.batch.tasklet.PlayerStatsAggregationTasklet
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class StatsAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val playerStatsTasklet: PlayerStatsAggregationTasklet,
    private val championStatsTasklet: ChampionStatsAggregationTasklet,
    private val championItemStatsTasklet: ChampionItemStatsAggregationTasklet,
) {
    companion object {
        const val JOB_NAME = "statsAggregationJob"
    }

    @Bean
    fun statsAggregationJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(playerStatsStep())
            .next(championStatsStep())
            .next(championItemStatsStep())
            .build()

    @Bean
    fun playerStatsStep(): Step =
        StepBuilder("playerStatsStep", jobRepository)
            .tasklet(playerStatsTasklet, transactionManager)
            .build()

    @Bean
    fun championStatsStep(): Step =
        StepBuilder("championStatsStep", jobRepository)
            .tasklet(championStatsTasklet, transactionManager)
            .build()

    @Bean
    fun championItemStatsStep(): Step =
        StepBuilder("championItemStatsStep", jobRepository)
            .tasklet(championItemStatsTasklet, transactionManager)
            .build()
}
