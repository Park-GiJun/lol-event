package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetWeeklyAwardsUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.OverviewStatsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.WeeklyAwardsResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 전체 요약.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/overview` | 개요 지표 |
 * | GET | `/api/stats/awards` | 주간 어워드 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Overview Stats", description = "전체 요약 통계 API")
class OverviewStatsWebAdapter(
    private val getOverviewStatsUseCase: GetOverviewStatsUseCase,
    private val getWeeklyAwardsUseCase: GetWeeklyAwardsUseCase,
) {
    @GetMapping("/overview")
    @Operation(summary = "전체 내전 통계 개요", description = "챔피언 픽 통계, 명예의 전당, 오브젝트 집계 등 전반적인 통계를 반환합니다")
    fun getOverviewStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<OverviewStatsResponse> =
        CommonApiResponse.success(OverviewStatsResponse.from(getOverviewStatsUseCase.getOverviewStats(mode)))

    @GetMapping("/awards")
    @Operation(summary = "주간 어워드")
    fun getWeeklyAwards(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<WeeklyAwardsResponse> =
        CommonApiResponse.success(WeeklyAwardsResponse.from(getWeeklyAwardsUseCase.getWeeklyAwards(mode)))
}
