package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.ChaosMatchResult
import com.gijun.main.application.dto.stats.result.DefeatContributionResult
import com.gijun.main.application.dto.stats.result.GoldEfficiencyResult
import com.gijun.main.application.dto.stats.result.JungleDominanceResult
import com.gijun.main.application.dto.stats.result.MultiKillHighlightsResult
import com.gijun.main.application.dto.stats.result.PlaystyleDnaResult
import com.gijun.main.application.dto.stats.result.PositionBadgeResult
import com.gijun.main.application.dto.stats.result.PositionChampionPoolResult
import com.gijun.main.application.dto.stats.result.SupportImpactResult
import com.gijun.main.application.dto.stats.result.SurvivalIndexResult
import com.gijun.main.application.port.`in`.GetChaosMatchUseCase
import com.gijun.main.application.port.`in`.GetDefeatContributionUseCase
import com.gijun.main.application.port.`in`.GetGoldEfficiencyUseCase
import com.gijun.main.application.port.`in`.GetJungleDominanceUseCase
import com.gijun.main.application.port.`in`.GetMultiKillHighlightsUseCase
import com.gijun.main.application.port.`in`.GetPlaystyleDnaUseCase
import com.gijun.main.application.port.`in`.GetPositionBadgeUseCase
import com.gijun.main.application.port.`in`.GetPositionChampionPoolUseCase
import com.gijun.main.application.port.`in`.GetSupportImpactUseCase
import com.gijun.main.application.port.`in`.GetSurvivalIndexUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Gameplay Stats", description = "게임플레이 상세 통계 API")
@RestController
@RequestMapping("/api/stats")
class GameplayStatsWebAdapter(
    private val getMultiKillHighlightsUseCase: GetMultiKillHighlightsUseCase,
    private val getChaosMatchUseCase: GetChaosMatchUseCase,
    private val getDefeatContributionUseCase: GetDefeatContributionUseCase,
    private val getSurvivalIndexUseCase: GetSurvivalIndexUseCase,
    private val getJungleDominanceUseCase: GetJungleDominanceUseCase,
    private val getSupportImpactUseCase: GetSupportImpactUseCase,
    private val getGoldEfficiencyUseCase: GetGoldEfficiencyUseCase,
    private val getPlaystyleDnaUseCase: GetPlaystyleDnaUseCase,
    private val getPositionBadgeUseCase: GetPositionBadgeUseCase,
    private val getPositionChampionPoolUseCase: GetPositionChampionPoolUseCase,
) {
    @GetMapping("/multikill-highlights")
    fun getMultiKillHighlights(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<MultiKillHighlightsResult> =
        CommonApiResponse.success(getMultiKillHighlightsUseCase.getMultiKillHighlights(mode))

    @GetMapping("/chaos-match")
    fun getChaosMatch(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<ChaosMatchResult> =
        CommonApiResponse.success(getChaosMatchUseCase.getChaosMatch(mode))

    @GetMapping("/defeat-contribution")
    fun getDefeatContribution(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<DefeatContributionResult> =
        CommonApiResponse.success(getDefeatContributionUseCase.getDefeatContribution(mode))

    @GetMapping("/survival-index")
    fun getSurvivalIndex(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<SurvivalIndexResult> =
        CommonApiResponse.success(getSurvivalIndexUseCase.getSurvivalIndex(mode))

    @GetMapping("/jungle-dominance")
    fun getJungleDominance(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<JungleDominanceResult> =
        CommonApiResponse.success(getJungleDominanceUseCase.getJungleDominance(mode))

    @GetMapping("/support-impact")
    fun getSupportImpact(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<SupportImpactResult> =
        CommonApiResponse.success(getSupportImpactUseCase.getSupportImpact(mode))

    @GetMapping("/gold-efficiency")
    fun getGoldEfficiency(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<GoldEfficiencyResult> =
        CommonApiResponse.success(getGoldEfficiencyUseCase.getGoldEfficiency(mode))

    @GetMapping("/playstyle-dna")
    fun getPlaystyleDna(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<PlaystyleDnaResult> =
        CommonApiResponse.success(getPlaystyleDnaUseCase.getPlaystyleDna(mode))

    @GetMapping("/position-badge")
    fun getPositionBadge(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<PositionBadgeResult> =
        CommonApiResponse.success(getPositionBadgeUseCase.getPositionBadge(mode))

    @GetMapping("/position-champion-pool")
    fun getPositionChampionPool(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<PositionChampionPoolResult> =
        CommonApiResponse.success(getPositionChampionPoolUseCase.getPositionChampionPool(mode))
}
