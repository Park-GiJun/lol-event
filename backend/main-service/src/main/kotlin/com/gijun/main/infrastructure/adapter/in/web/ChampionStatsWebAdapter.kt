package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.ChampionCertificateResult
import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionMatchupResult
import com.gijun.main.application.dto.stats.result.ChampionTierResult
import com.gijun.main.application.port.`in`.GetChampionCertificateUseCase
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
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
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<ChampionCertificateResult> =
        CommonApiResponse.success(getChampionCertificateUseCase.getChampionCertificates(mode, minGames))

    @Operation(
        summary = "챔피언 상성",
        description = "champion=X: X의 라인전 지표와 상대별 상성 / vsChampion=X: X를 상대한 쪽의 성적(카운터).\n\n" +
            "같은 라인끼리만 맞춘다. 예전에는 상대 다섯 명 전부와 짝지어 탑과 상대 서포터가 상성으로 잡혔다.\n" +
            "laneStrength 는 챔피언 x 라인 단위라 표본이 두텁고, matchups 는 개별 상성이라 얇아 최소 표본을 넘긴 것만 나온다."
    )
    @GetMapping("/matchup")
    fun getMatchup(
        @RequestParam(required = false) champion: String?,
        @RequestParam(required = false) vsChampion: String?,
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<ChampionMatchupResult> =
        CommonApiResponse.success(getChampionMatchupUseCase.getMatchup(
            champion?.let { java.net.URLDecoder.decode(it, "UTF-8") },
            vsChampion?.let { java.net.URLDecoder.decode(it, "UTF-8") },
            mode,
        ))
}
