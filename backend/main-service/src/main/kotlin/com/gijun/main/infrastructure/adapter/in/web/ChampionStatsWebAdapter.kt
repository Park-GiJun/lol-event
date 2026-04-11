package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.ChampionCertificateResult
import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionMatchupResult
import com.gijun.main.application.dto.stats.result.ChampionSynergyResult
import com.gijun.main.application.dto.stats.result.ChampionTierResult
import com.gijun.main.application.port.`in`.GetChampionCertificateUseCase
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetChampionSynergyUseCase
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Champion Stats", description = "챔피언별 통계 API")
@RestController
@RequestMapping("/api/stats")
class ChampionStatsWebAdapter(
    private val getChampionStatsUseCase: GetChampionStatsUseCase,
    private val getChampionSynergyUseCase: GetChampionSynergyUseCase,
    private val getChampionMatchupUseCase: GetChampionMatchupUseCase,
    private val getChampionTierUseCase: GetChampionTierUseCase,
    private val getChampionCertificateUseCase: GetChampionCertificateUseCase,
) {
    @Operation(summary = "챔피언 장인 랭킹", description = "특정 챔피언을 플레이한 멤버별 통계를 반환합니다")
    @GetMapping("/champion/{champion}")
    fun getChampionStats(
        @Parameter(description = "챔피언 이름 (URL 인코딩)", example = "Jinx")
        @PathVariable champion: String,
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<ChampionDetailStats> =
        CommonApiResponse.success(getChampionStatsUseCase.getChampionStats(
            java.net.URLDecoder.decode(champion, "UTF-8"), mode
        ))

    @Operation(summary = "챔피언 티어 리스트")
    @GetMapping("/champion-tier")
    fun getChampionTier(
        @RequestParam(defaultValue = "normal") mode: String,
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<ChampionTierResult> =
        CommonApiResponse.success(getChampionTierUseCase.getChampionTier(mode, minGames))

    @GetMapping("/champion-certificate")
    fun getChampionCertificate(
        @RequestParam(defaultValue = "normal") mode: String,
        @RequestParam(defaultValue = "5") minGames: Int,
    ): CommonApiResponse<ChampionCertificateResult> =
        CommonApiResponse.success(getChampionCertificateUseCase.getChampionCertificates(mode, minGames))

    @Operation(
        summary = "챔피언 상성 매트릭스",
        description = "champion=X: X의 각 상대 챔피언 vs 승률 / vsChampion=X: X를 상대하는 챔피언 중 카운터픽 추천"
    )
    @GetMapping("/matchup")
    fun getMatchup(
        @RequestParam(required = false) champion: String?,
        @RequestParam(required = false) vsChampion: String?,
        @RequestParam(defaultValue = "normal") mode: String,
        @RequestParam(defaultValue = "false") samePosition: Boolean,
    ): CommonApiResponse<ChampionMatchupResult> =
        CommonApiResponse.success(getChampionMatchupUseCase.getMatchup(
            champion?.let { java.net.URLDecoder.decode(it, "UTF-8") },
            vsChampion?.let { java.net.URLDecoder.decode(it, "UTF-8") },
            mode,
            samePosition,
        ))

    @Operation(
        summary = "챔피언 조합 시너지 분석",
        description = "같은 팀에서 함께 플레이한 챔피언 조합의 승률을 반환합니다"
    )
    @GetMapping("/synergy")
    fun getChampionSynergy(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String,
        @Parameter(description = "최소 게임 수 필터", example = "3")
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<ChampionSynergyResult> =
        CommonApiResponse.success(getChampionSynergyUseCase.getChampionSynergy(mode, minGames))
}
