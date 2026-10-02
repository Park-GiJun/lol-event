package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.port.`in`.GetChaosMatchUseCase
import com.gijun.main.application.port.`in`.GetDamageAnalysisUseCase
import com.gijun.main.application.port.`in`.GetLateGameUseCase
import com.gijun.main.application.port.`in`.GetMultiKillHighlightsUseCase
import com.gijun.main.application.port.`in`.GetSurrenderAnalysisUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChaosMatchResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.DamageAnalysisResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.LateGameResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.MultiKillHighlightsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.SurrenderAnalysisResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 교전 통계.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/multikill-highlights` | 멀티킬 하이라이트 |
 * | GET | `/api/stats/chaos-match` | 난전 경기 |
 * | GET | `/api/stats/damage-analysis` | 딜 분석 |
 * | GET | `/api/stats/surrender-analysis` | 항복 분석 |
 * | GET | `/api/stats/late-game` | 후반 지표 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Combat Stats", description = "교전 통계 API")
class CombatStatsWebAdapter(
    private val getMultiKillHighlightsUseCase: GetMultiKillHighlightsUseCase,
    private val getChaosMatchUseCase: GetChaosMatchUseCase,
    private val getDamageAnalysisUseCase: GetDamageAnalysisUseCase,
    private val getSurrenderAnalysisUseCase: GetSurrenderAnalysisUseCase,
    private val getLateGameUseCase: GetLateGameUseCase,
) {
    @GetMapping("/multikill-highlights")
    @Operation(summary = "멀티킬 하이라이트")
    fun getMultiKillHighlights(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<MultiKillHighlightsResponse> =
        CommonApiResponse.success(MultiKillHighlightsResponse.from(getMultiKillHighlightsUseCase.getMultiKillHighlights(mode)))

    @GetMapping("/chaos-match")
    @Operation(summary = "난전 경기")
    fun getChaosMatch(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<ChaosMatchResponse> = CommonApiResponse.success(ChaosMatchResponse.from(getChaosMatchUseCase.getChaosMatch(mode)))

    @GetMapping("/damage-analysis")
    @Operation(summary = "딜 분석")
    fun getDamageAnalysis(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<DamageAnalysisResponse> =
        CommonApiResponse.success(DamageAnalysisResponse.from(getDamageAnalysisUseCase.getDamageAnalysis(mode)))

    @GetMapping("/surrender-analysis")
    @Operation(summary = "항복 분석")
    fun getSurrenderAnalysis(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<SurrenderAnalysisResponse> =
        CommonApiResponse.success(SurrenderAnalysisResponse.from(getSurrenderAnalysisUseCase.getSurrenderAnalysis(mode)))

    @GetMapping("/late-game")
    @Operation(summary = "후반 지표")
    fun getLateGame(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<LateGameResponse> = CommonApiResponse.success(LateGameResponse.from(getLateGameUseCase.getLateGame(mode)))
}
