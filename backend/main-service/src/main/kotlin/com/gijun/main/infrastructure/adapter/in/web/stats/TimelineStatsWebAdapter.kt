package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.dto.query.GetTimelineChampionsQuery
import com.gijun.main.application.dto.query.GetTimelineLaneQuery
import com.gijun.main.application.port.`in`.GetTimelineChampionsUseCase
import com.gijun.main.application.port.`in`.GetTimelineLaneUseCase
import com.gijun.main.application.port.`in`.GetTimelineStatsUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.TimelineChampionsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.TimelineLaneResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.TimelineStatsResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 타임라인 지표. 타임라인이 있는 경기만 대상이다.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/timeline` | 전체 |
 * | GET | `/api/stats/timeline/lane` | 라인별 |
 * | GET | `/api/stats/timeline/champions` | 챔피언별 |
 * ```
 * 개인 타임라인(`/api/stats/player/{riotId}/timeline`)은 [PlayerStatsWebAdapter] 에 있다.
 */
@RestController
@RequestMapping("/api/stats/timeline", version = "1.0")
@Tag(name = "Timeline Stats", description = "타임라인 지표 API")
class TimelineStatsWebAdapter(
    private val getTimelineStatsUseCase: GetTimelineStatsUseCase,
    private val getTimelineLaneUseCase: GetTimelineLaneUseCase,
    private val getTimelineChampionsUseCase: GetTimelineChampionsUseCase,
) {
    @GetMapping
    @Operation(
        summary = "타임라인 지표",
        description = "타임라인이 있는 경기만 대상으로 15분 라인 격차, 초반 킬·데스, 퍼블 관여, 15분 골드 리드 팀 승률을 반환합니다",
    )
    fun getTimelineStats(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<TimelineStatsResponse> =
        CommonApiResponse.success(TimelineStatsResponse.from(getTimelineStatsUseCase.getTimelineStats(mode)))

    @GetMapping("/lane")
    @Operation(summary = "라인별 타임라인 지표", description = "그 라인에서 뛴 경기만 센 라인 평균과 선수별 15분 지표")
    fun getTimelineLane(
        @Parameter(description = "TOP / JUNGLE / MID / ADC / SUPPORT", example = "TOP")
        @RequestParam lane: String,
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<TimelineLaneResponse> =
        CommonApiResponse.success(
            TimelineLaneResponse.from(getTimelineLaneUseCase.getTimelineLane(GetTimelineLaneQuery(lane, mode))),
        )

    @GetMapping("/champions")
    @Operation(summary = "챔피언별 타임라인 지표", description = "챔피언별·챔피언×포지션별 15분 지표. champion 을 주면 그 챔피언만")
    fun getTimelineChampions(
        @RequestParam(defaultValue = "normal") mode: GameMode,
        @Parameter(description = "챔피언 영문명", example = "Ahri")
        @RequestParam(required = false) champion: String?,
    ): CommonApiResponse<TimelineChampionsResponse> =
        CommonApiResponse.success(
            TimelineChampionsResponse.from(
                getTimelineChampionsUseCase.getTimelineChampions(GetTimelineChampionsQuery(mode, champion)),
            ),
        )
}
