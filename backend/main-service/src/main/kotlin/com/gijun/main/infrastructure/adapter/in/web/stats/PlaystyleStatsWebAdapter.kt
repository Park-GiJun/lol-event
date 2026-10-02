package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.port.`in`.GetDefeatContributionUseCase
import com.gijun.main.application.port.`in`.GetPlaystyleDnaUseCase
import com.gijun.main.application.port.`in`.GetPositionBadgeUseCase
import com.gijun.main.application.port.`in`.GetPositionChampionPoolUseCase
import com.gijun.main.application.port.`in`.GetSurvivalIndexUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.DefeatContributionResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.PlaystyleDnaResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.PositionBadgeResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.PositionChampionPoolResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.SurvivalIndexResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 플레이 성향 통계.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/playstyle-dna` | 성향 DNA |
 * | GET | `/api/stats/position-badge` | 포지션 배지 |
 * | GET | `/api/stats/position-champion-pool` | 포지션별 챔피언 풀 |
 * | GET | `/api/stats/survival-index` | 생존 지수 |
 * | GET | `/api/stats/defeat-contribution` | 패배 기여 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Playstyle Stats", description = "플레이 성향 통계 API")
class PlaystyleStatsWebAdapter(
    private val getPlaystyleDnaUseCase: GetPlaystyleDnaUseCase,
    private val getPositionBadgeUseCase: GetPositionBadgeUseCase,
    private val getPositionChampionPoolUseCase: GetPositionChampionPoolUseCase,
    private val getSurvivalIndexUseCase: GetSurvivalIndexUseCase,
    private val getDefeatContributionUseCase: GetDefeatContributionUseCase,
) {
    @GetMapping("/playstyle-dna")
    @Operation(summary = "플레이 성향 DNA", description = "공격성·생존력·팀플레이·오브젝트·경제·시야 여섯 축")
    fun getPlaystyleDna(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<PlaystyleDnaResponse> =
        CommonApiResponse.success(PlaystyleDnaResponse.from(getPlaystyleDnaUseCase.getPlaystyleDna(mode)))

    @GetMapping("/position-badge")
    @Operation(summary = "포지션 배지")
    fun getPositionBadge(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<PositionBadgeResponse> =
        CommonApiResponse.success(PositionBadgeResponse.from(getPositionBadgeUseCase.getPositionBadge(mode)))

    @GetMapping("/position-champion-pool")
    @Operation(summary = "포지션별 챔피언 풀")
    fun getPositionChampionPool(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<PositionChampionPoolResponse> =
        CommonApiResponse.success(PositionChampionPoolResponse.from(getPositionChampionPoolUseCase.getPositionChampionPool(mode)))

    @GetMapping("/survival-index")
    @Operation(summary = "생존 지수")
    fun getSurvivalIndex(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<SurvivalIndexResponse> =
        CommonApiResponse.success(SurvivalIndexResponse.from(getSurvivalIndexUseCase.getSurvivalIndex(mode)))

    @GetMapping("/defeat-contribution")
    @Operation(summary = "패배 기여")
    fun getDefeatContribution(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<DefeatContributionResponse> =
        CommonApiResponse.success(DefeatContributionResponse.from(getDefeatContributionUseCase.getDefeatContribution(mode)))
}
