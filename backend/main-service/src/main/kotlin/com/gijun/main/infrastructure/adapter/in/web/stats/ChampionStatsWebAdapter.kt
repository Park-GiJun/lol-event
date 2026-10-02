package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.dto.query.GetChampionCertificateQuery
import com.gijun.main.application.dto.query.GetChampionMatchupQuery
import com.gijun.main.application.dto.query.GetChampionStatsQuery
import com.gijun.main.application.dto.query.GetChampionTierQuery
import com.gijun.main.application.port.`in`.GetBanAnalysisUseCase
import com.gijun.main.application.port.`in`.GetChampionCertificateUseCase
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.BanAnalysisResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChampionCertificateResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChampionDetailStatsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChampionMatchupResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.ChampionTierResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import com.gijun.main.shared.infrastructure.web.common.urlDecoded
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 챔피언 축 통계.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/champion/{champion}` | 챔피언 상세 |
 * | GET | `/api/stats/champion-tier` | 티어 리스트 |
 * | GET | `/api/stats/champion-certificate` | 장인 인증 |
 * | GET | `/api/stats/matchup` | 상성 |
 * | GET | `/api/stats/ban-analysis` | 밴 분석 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Champion Stats", description = "챔피언별 통계 API")
class ChampionStatsWebAdapter(
    private val getChampionStatsUseCase: GetChampionStatsUseCase,
    private val getChampionTierUseCase: GetChampionTierUseCase,
    private val getChampionCertificateUseCase: GetChampionCertificateUseCase,
    private val getChampionMatchupUseCase: GetChampionMatchupUseCase,
    private val getBanAnalysisUseCase: GetBanAnalysisUseCase,
) {
    @GetMapping("/champion/{champion}")
    @Operation(summary = "챔피언 장인 랭킹", description = "특정 챔피언을 플레이한 멤버별 통계를 반환합니다")
    fun getChampionStats(
        @Parameter(description = "챔피언 이름 (URL 인코딩)", example = "Jinx")
        @PathVariable champion: String,
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<ChampionDetailStatsResponse> =
        CommonApiResponse.success(
            ChampionDetailStatsResponse.from(
                getChampionStatsUseCase.getChampionStats(GetChampionStatsQuery(urlDecoded(champion), mode)),
            ),
        )

    @GetMapping("/champion-tier")
    @Operation(summary = "챔피언 티어 리스트")
    fun getChampionTier(
        @RequestParam(defaultValue = "normal") mode: GameMode,
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<ChampionTierResponse> =
        CommonApiResponse.success(
            ChampionTierResponse.from(getChampionTierUseCase.getChampionTier(GetChampionTierQuery(mode, minGames))),
        )

    @GetMapping("/champion-certificate")
    @Operation(summary = "챔피언 장인 인증", description = "표본을 50% 쪽으로 당긴 승률이 50 을 넘는 조합만 인증합니다")
    fun getChampionCertificate(
        @RequestParam(defaultValue = "normal") mode: GameMode,
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<ChampionCertificateResponse> =
        CommonApiResponse.success(
            ChampionCertificateResponse.from(
                getChampionCertificateUseCase.getChampionCertificate(GetChampionCertificateQuery(mode, minGames)),
            ),
        )

    @GetMapping("/matchup")
    @Operation(
        summary = "챔피언 상성",
        description =
            "champion=X: X의 라인전 지표와 상대별 상성 / vsChampion=X: X를 상대한 쪽의 성적(카운터).\n\n" +
                "같은 라인끼리만 맞춘다. 예전에는 상대 다섯 명 전부와 짝지어 탑과 상대 서포터가 상성으로 잡혔다.\n" +
                "laneStrength 는 챔피언 x 라인 단위라 표본이 두텁고, matchups 는 개별 상성이라 얇아 최소 표본을 넘긴 것만 나온다.",
    )
    fun getChampionMatchup(
        @RequestParam(required = false) champion: String?,
        @RequestParam(required = false) vsChampion: String?,
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<ChampionMatchupResponse> =
        CommonApiResponse.success(
            ChampionMatchupResponse.from(
                getChampionMatchupUseCase.getChampionMatchup(
                    GetChampionMatchupQuery(
                        champion = champion?.let(::urlDecoded),
                        vsChampion = vsChampion?.let(::urlDecoded),
                        mode = mode,
                    ),
                ),
            ),
        )

    @GetMapping("/ban-analysis")
    @Operation(summary = "밴 분석", description = "가장 많이 밴된 챔피언과 밴률")
    fun getBanAnalysis(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<BanAnalysisResponse> =
        CommonApiResponse.success(BanAnalysisResponse.from(getBanAnalysisUseCase.getBanAnalysis(mode)))
}
