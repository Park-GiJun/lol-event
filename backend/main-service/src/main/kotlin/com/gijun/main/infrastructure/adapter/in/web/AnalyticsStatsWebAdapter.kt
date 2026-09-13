package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.BanAnalysisResult
import com.gijun.main.application.dto.stats.result.ComebackIndexResult
import com.gijun.main.application.dto.stats.result.EarlyGameDominanceResult
import com.gijun.main.application.dto.stats.result.GameLengthTendencyResult
import com.gijun.main.application.dto.stats.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.SessionReportResult
import com.gijun.main.application.dto.stats.result.TimePatternResult
import com.gijun.main.application.dto.stats.result.TimelineChampionsResult
import com.gijun.main.application.dto.stats.result.TimelineLaneResult
import com.gijun.main.application.dto.stats.result.TimelineStatsResult
import com.gijun.main.application.dto.stats.result.WeeklyAwardsResult
import com.gijun.main.application.port.`in`.GetBanAnalysisUseCase
import com.gijun.main.application.port.`in`.GetComebackIndexUseCase
import com.gijun.main.application.port.`in`.GetEarlyGameDominanceUseCase
import com.gijun.main.application.port.`in`.GetGameLengthTendencyUseCase
import com.gijun.main.application.port.`in`.GetObjectiveCorrelationUseCase
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetSessionReportUseCase
import com.gijun.main.application.port.`in`.GetTimePatternUseCase
import com.gijun.main.application.port.`in`.GetTimelineStatsUseCase
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
    private val getSessionReportUseCase: GetSessionReportUseCase,
    private val getTimePatternUseCase: GetTimePatternUseCase,
    private val getGameLengthTendencyUseCase: GetGameLengthTendencyUseCase,
    private val getEarlyGameDominanceUseCase: GetEarlyGameDominanceUseCase,
    private val getComebackIndexUseCase: GetComebackIndexUseCase,
    private val getBanAnalysisUseCase: GetBanAnalysisUseCase,
    private val getObjectiveCorrelationUseCase: GetObjectiveCorrelationUseCase,
    private val getTimelineStatsUseCase: GetTimelineStatsUseCase,
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

    @Operation(
        summary = "타임라인 지표",
        description = "타임라인이 있는 경기만 대상으로 15분 라인 격차, 초반 킬·데스, 퍼블 관여, 15분 골드 리드 팀 승률을 반환합니다"
    )
    @GetMapping("/timeline")
    fun getTimelineStats(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<TimelineStatsResult> =
        CommonApiResponse.success(getTimelineStatsUseCase.getTimelineStats(mode))

    @Operation(summary = "라인별 타임라인 지표", description = "그 라인에서 뛴 경기만 센 라인 평균과 선수별 15분 지표")
    @GetMapping("/timeline/lane")
    fun getTimelineLane(
        @Parameter(description = "TOP / JUNGLE / MID / ADC / SUPPORT", example = "TOP")
        @RequestParam lane: String,
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<TimelineLaneResult> =
        CommonApiResponse.success(getTimelineStatsUseCase.getTimelineLane(lane, mode))

    @Operation(summary = "챔피언별 타임라인 지표", description = "챔피언별·챔피언×포지션별 15분 지표. champion 을 주면 그 챔피언만")
    @GetMapping("/timeline/champions")
    fun getTimelineChampions(
        @RequestParam(defaultValue = "normal") mode: String,
        @Parameter(description = "챔피언 영문명", example = "Ahri")
        @RequestParam(required = false) champion: String?,
    ): CommonApiResponse<TimelineChampionsResult> =
        CommonApiResponse.success(getTimelineStatsUseCase.getTimelineChampions(mode, champion))
}
