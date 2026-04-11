package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.BanAnalysisResult
import com.gijun.main.application.dto.stats.result.ComebackIndexResult
import com.gijun.main.application.dto.stats.result.EarlyGameDominanceResult
import com.gijun.main.application.dto.stats.result.GameLengthTendencyResult
import com.gijun.main.application.dto.stats.result.MetaShiftResult
import com.gijun.main.application.dto.stats.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.SessionReportResult
import com.gijun.main.application.dto.stats.result.TimePatternResult
import com.gijun.main.application.dto.stats.result.WeeklyAwardsResult
import com.gijun.main.application.port.`in`.GetBanAnalysisUseCase
import com.gijun.main.application.port.`in`.GetComebackIndexUseCase
import com.gijun.main.application.port.`in`.GetEarlyGameDominanceUseCase
import com.gijun.main.application.port.`in`.GetGameLengthTendencyUseCase
import com.gijun.main.application.port.`in`.GetMetaShiftUseCase
import com.gijun.main.application.port.`in`.GetObjectiveCorrelationUseCase
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetSessionReportUseCase
import com.gijun.main.application.port.`in`.GetTimePatternUseCase
import com.gijun.main.application.port.`in`.GetWeeklyAwardsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Analytics Stats", description = "분석/메타 통계 API")
@RestController
@RequestMapping("/api/stats")
class AnalyticsStatsWebAdapter(
    private val getOverviewStatsUseCase: GetOverviewStatsUseCase,
    private val getWeeklyAwardsUseCase: GetWeeklyAwardsUseCase,
    private val getMetaShiftUseCase: GetMetaShiftUseCase,
    private val getSessionReportUseCase: GetSessionReportUseCase,
    private val getTimePatternUseCase: GetTimePatternUseCase,
    private val getGameLengthTendencyUseCase: GetGameLengthTendencyUseCase,
    private val getEarlyGameDominanceUseCase: GetEarlyGameDominanceUseCase,
    private val getComebackIndexUseCase: GetComebackIndexUseCase,
    private val getBanAnalysisUseCase: GetBanAnalysisUseCase,
    private val getObjectiveCorrelationUseCase: GetObjectiveCorrelationUseCase,
) {
    @Operation(summary = "전체 내전 통계 개요", description = "챔피언 픽 통계, 명예의 전당, 오브젝트 집계 등 전반적인 통계를 반환합니다")
    @GetMapping("/overview")
    fun getOverview(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String
    ): CommonApiResponse<OverviewStats> =
        CommonApiResponse.success(getOverviewStatsUseCase.getOverviewStats(mode))

    @GetMapping("/awards")
    fun getWeeklyAwards(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<WeeklyAwardsResult> =
        CommonApiResponse.success(getWeeklyAwardsUseCase.getWeeklyAwards(mode))

    @GetMapping("/meta-shift")
    fun getMetaShift(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<MetaShiftResult> =
        CommonApiResponse.success(getMetaShiftUseCase.getMetaShift(mode))

    @GetMapping("/sessions")
    fun getSessionReport(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<SessionReportResult> =
        CommonApiResponse.success(getSessionReportUseCase.getSessionReport(mode))

    @GetMapping("/time-pattern")
    fun getTimePattern(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<TimePatternResult> =
        CommonApiResponse.success(getTimePatternUseCase.getTimePattern(mode))

    @GetMapping("/game-length-tendency")
    fun getGameLengthTendency(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<GameLengthTendencyResult> =
        CommonApiResponse.success(getGameLengthTendencyUseCase.getGameLengthTendency(mode))

    @GetMapping("/early-game")
    fun getEarlyGameDominance(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<EarlyGameDominanceResult> =
        CommonApiResponse.success(getEarlyGameDominanceUseCase.getEarlyGameDominance(mode))

    @GetMapping("/comeback")
    fun getComebackIndex(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<ComebackIndexResult> =
        CommonApiResponse.success(getComebackIndexUseCase.getComebackIndex(mode))

    @GetMapping("/ban-analysis")
    fun getBanAnalysis(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<BanAnalysisResult> =
        CommonApiResponse.success(getBanAnalysisUseCase.getBanAnalysis(mode))

    @Operation(
        summary = "오브젝트 선점 → 승률 상관관계",
        description = "퍼블/드래곤/바론/포탑 선점팀의 승률과 미선점팀 승률 비교"
    )
    @GetMapping("/objectives")
    fun getObjectiveCorrelation(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<ObjectiveCorrelationResult> =
        CommonApiResponse.success(getObjectiveCorrelationUseCase.getObjectiveCorrelation(mode))
}
