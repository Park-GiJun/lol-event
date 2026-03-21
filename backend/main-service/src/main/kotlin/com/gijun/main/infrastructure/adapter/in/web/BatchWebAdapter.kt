package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.ChampionItemStatsCacheRepository
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.ChampionStatsCacheRepository
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.PlayerStatsCacheRepository
import com.gijun.main.infrastructure.adapter.`in`.scheduler.StatsAggregationScheduler
import com.gijun.main.infrastructure.batch.tasklet.ChampionItemStatsAggregationTasklet
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@Tag(name = "Batch", description = "통계 배치 관리 API")
@RestController
@RequestMapping("/api/batch")
class BatchWebAdapter(
    private val scheduler: StatsAggregationScheduler,
    private val playerStatsCacheRepository: PlayerStatsCacheRepository,
    private val championStatsCacheRepository: ChampionStatsCacheRepository,
    private val championItemStatsCacheRepository: ChampionItemStatsCacheRepository,
    private val championItemStatsAggregationTasklet: ChampionItemStatsAggregationTasklet,
) {
    data class BatchStatusResponse(
        val playerSnapshotCount: Long,
        val championSnapshotCount: Long,
        val championItemSnapshotCount: Long,
        val lastAggregatedAt: LocalDateTime?,
        val message: String,
    )

    @Operation(summary = "배치 상태 조회", description = "스냅샷 테이블 현황과 마지막 집계 시각을 반환합니다")
    @GetMapping("/status")
    fun getStatus(): CommonApiResponse<BatchStatusResponse> {
        val playerCount     = playerStatsCacheRepository.count()
        val champCount      = championStatsCacheRepository.count()
        val itemCount       = championItemStatsCacheRepository.count()
        val lastAt = playerStatsCacheRepository.findAllByMode("normal")
            .maxOfOrNull { it.aggregatedAt }
        return CommonApiResponse.success(
            BatchStatusResponse(playerCount, champCount, itemCount, lastAt,
                if (lastAt != null) "마지막 집계: $lastAt" else "아직 집계된 데이터가 없습니다")
        )
    }

    @Operation(summary = "배치 수동 실행", description = "통계 집계 배치 잡을 즉시 실행합니다")
    @PostMapping("/trigger")
    fun triggerBatch(): CommonApiResponse<String> {
        scheduler.launchJob(reason = "manual-api")
        return CommonApiResponse.success("통계 집계 배치가 시작되었습니다")
    }

    @Operation(summary = "챔피언 아이템 통계 수동 집계", description = "챔피언별 아이템 통계 스냅샷을 즉시 재집계합니다")
    @PostMapping("/trigger-item-stats")
    fun triggerItemStats(): CommonApiResponse<String> {
        championItemStatsAggregationTasklet.aggregate()
        return CommonApiResponse.success("챔피언 아이템 통계 집계가 완료되었습니다")
    }
}
