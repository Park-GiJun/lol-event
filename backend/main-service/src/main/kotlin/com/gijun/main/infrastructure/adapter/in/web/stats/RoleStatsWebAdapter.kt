package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.port.`in`.GetGoldEfficiencyUseCase
import com.gijun.main.application.port.`in`.GetJungleDominanceUseCase
import com.gijun.main.application.port.`in`.GetSupportImpactUseCase
import com.gijun.main.application.port.`in`.GetVisionDominanceUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.GoldEfficiencyResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.JungleDominanceResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.SupportImpactResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.VisionDominanceResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 역할 수행 통계.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/jungle-dominance` | 정글 장악 |
 * | GET | `/api/stats/support-impact` | 서포터 영향력 |
 * | GET | `/api/stats/vision-dominance` | 시야 장악 |
 * | GET | `/api/stats/gold-efficiency` | 골드 효율 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Role Stats", description = "역할 수행 통계 API")
class RoleStatsWebAdapter(
    private val getJungleDominanceUseCase: GetJungleDominanceUseCase,
    private val getSupportImpactUseCase: GetSupportImpactUseCase,
    private val getVisionDominanceUseCase: GetVisionDominanceUseCase,
    private val getGoldEfficiencyUseCase: GetGoldEfficiencyUseCase,
) {
    @GetMapping("/jungle-dominance")
    @Operation(summary = "정글 장악")
    fun getJungleDominance(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<JungleDominanceResponse> =
        CommonApiResponse.success(JungleDominanceResponse.from(getJungleDominanceUseCase.getJungleDominance(mode)))

    @GetMapping("/support-impact")
    @Operation(summary = "서포터 영향력")
    fun getSupportImpact(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<SupportImpactResponse> =
        CommonApiResponse.success(SupportImpactResponse.from(getSupportImpactUseCase.getSupportImpact(mode)))

    @GetMapping("/vision-dominance")
    @Operation(summary = "시야 장악")
    fun getVisionDominance(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<VisionDominanceResponse> =
        CommonApiResponse.success(VisionDominanceResponse.from(getVisionDominanceUseCase.getVisionDominance(mode)))

    @GetMapping("/gold-efficiency")
    @Operation(summary = "골드 효율")
    fun getGoldEfficiency(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<GoldEfficiencyResponse> =
        CommonApiResponse.success(GoldEfficiencyResponse.from(getGoldEfficiencyUseCase.getGoldEfficiency(mode)))
}
