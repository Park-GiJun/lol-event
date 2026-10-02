package com.gijun.main.infrastructure.adapter.`in`.web.batch

import com.gijun.main.application.port.`in`.AggregateChampionItemStatsUseCase
import com.gijun.main.application.port.`in`.GetStatsAggregationStatusUseCase
import com.gijun.main.application.port.`in`.TriggerStatsAggregationUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.batch.dto.StatsAggregationStatusResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 통계 스냅샷 집계 — 운영자가 손으로 돌리는 진입점.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/batch/status` | 스냅샷 현황 |
 * | POST | `/api/batch/trigger` | 전체 집계 잡 시작(비동기) |
 * | POST | `/api/batch/trigger-item-stats` | 챔피언 아이템 통계만 즉시 재집계(동기) |
 * ```
 * 정기 실행은 `StatsAggregationScheduler`, 경기 저장 뒤의 실행은 `StatsRebuildConsumer` 가 맡는다.
 * 셋 다 같은 유즈케이스를 부른다.
 */
@RestController
@RequestMapping("/api/batch", version = "1.0")
@Tag(name = "Batch", description = "통계 배치 관리 API")
class BatchWebAdapter(
    private val getStatsAggregationStatusUseCase: GetStatsAggregationStatusUseCase,
    private val triggerStatsAggregationUseCase: TriggerStatsAggregationUseCase,
    private val aggregateChampionItemStatsUseCase: AggregateChampionItemStatsUseCase,
) {
    // ===== 조회 =====

    @GetMapping("/status")
    @Operation(summary = "배치 상태 조회", description = "스냅샷 테이블 현황과 마지막 집계 시각을 반환합니다")
    fun getStatsAggregationStatus(): CommonApiResponse<StatsAggregationStatusResponse> =
        CommonApiResponse.success(StatsAggregationStatusResponse.from(getStatsAggregationStatusUseCase.getStatsAggregationStatus()))

    // ===== 변경 =====

    @PostMapping("/trigger")
    @Operation(summary = "배치 수동 실행", description = "통계 집계 배치 잡을 즉시 실행합니다")
    fun triggerStatsAggregation(): CommonApiResponse<String> {
        triggerStatsAggregationUseCase.triggerStatsAggregation(REASON)
        return CommonApiResponse.success("통계 집계 배치가 시작되었습니다")
    }

    @PostMapping("/trigger-item-stats")
    @Operation(summary = "챔피언 아이템 통계 수동 집계", description = "챔피언별 아이템 통계 스냅샷을 즉시 재집계합니다")
    fun aggregateChampionItemStats(): CommonApiResponse<String> {
        aggregateChampionItemStatsUseCase.aggregateChampionItemStats()
        return CommonApiResponse.success("챔피언 아이템 통계 집계가 완료되었습니다")
    }

    private companion object {
        private const val REASON = "manual-api"
    }
}
