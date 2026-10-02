package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.port.`in`.GetComebackIndexUseCase
import com.gijun.main.application.port.`in`.GetEarlyGameDominanceUseCase
import com.gijun.main.application.port.`in`.GetGameLengthTendencyUseCase
import com.gijun.main.application.port.`in`.GetObjectiveCorrelationUseCase
import com.gijun.main.application.port.`in`.GetTimePatternUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ComebackIndexResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.EarlyGameDominanceResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.GameLengthTendencyResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ObjectiveCorrelationResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.TimePatternResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 경기 흐름 통계.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/time-pattern` | 시간대 패턴 |
 * | GET | `/api/stats/game-length-tendency` | 경기 길이 성향 |
 * | GET | `/api/stats/early-game` | 초반 지배 |
 * | GET | `/api/stats/comeback` | 역전 지수 |
 * | GET | `/api/stats/objectives` | 오브젝트 선점과 승률 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Game Flow Stats", description = "경기 흐름 통계 API")
class GameFlowStatsWebAdapter(
    private val getTimePatternUseCase: GetTimePatternUseCase,
    private val getGameLengthTendencyUseCase: GetGameLengthTendencyUseCase,
    private val getEarlyGameDominanceUseCase: GetEarlyGameDominanceUseCase,
    private val getComebackIndexUseCase: GetComebackIndexUseCase,
    private val getObjectiveCorrelationUseCase: GetObjectiveCorrelationUseCase,
) {
    @GetMapping("/time-pattern")
    @Operation(summary = "시간대 패턴")
    fun getTimePattern(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<TimePatternResponse> =
        CommonApiResponse.success(TimePatternResponse.from(getTimePatternUseCase.getTimePattern(mode)))

    @GetMapping("/game-length-tendency")
    @Operation(summary = "경기 길이 성향")
    fun getGameLengthTendency(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<GameLengthTendencyResponse> =
        CommonApiResponse.success(GameLengthTendencyResponse.from(getGameLengthTendencyUseCase.getGameLengthTendency(mode)))

    @GetMapping("/early-game")
    @Operation(summary = "초반 지배")
    fun getEarlyGameDominance(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<EarlyGameDominanceResponse> =
        CommonApiResponse.success(EarlyGameDominanceResponse.from(getEarlyGameDominanceUseCase.getEarlyGameDominance(mode)))

    @GetMapping("/comeback")
    @Operation(summary = "역전 지수")
    fun getComebackIndex(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<ComebackIndexResponse> =
        CommonApiResponse.success(ComebackIndexResponse.from(getComebackIndexUseCase.getComebackIndex(mode)))

    @GetMapping("/objectives")
    @Operation(
        summary = "오브젝트 선점 → 승률 상관관계",
        description = "퍼블/드래곤/바론/포탑 선점팀의 승률과 미선점팀 승률 비교",
    )
    fun getObjectiveCorrelation(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<ObjectiveCorrelationResponse> =
        CommonApiResponse.success(ObjectiveCorrelationResponse.from(getObjectiveCorrelationUseCase.getObjectiveCorrelation(mode)))
}
