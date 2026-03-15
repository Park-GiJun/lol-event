package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.StatsResult
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.`in`.GetStatsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Stats", description = "내전 통계 API")
@RestController
@RequestMapping("/api/stats")
class StatsWebAdapter(
    private val getStatsUseCase: GetStatsUseCase,
    private val getPlayerStatsUseCase: GetPlayerStatsUseCase,
    private val getOverviewStatsUseCase: GetOverviewStatsUseCase,
    private val getChampionStatsUseCase: GetChampionStatsUseCase,
) {
    @Operation(summary = "전체 내전 통계 개요", description = "챔피언 픽 통계, 명예의 전당, 오브젝트 집계 등 전반적인 통계를 반환합니다")
    @GetMapping("/overview")
    fun getOverview(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String
    ): CommonApiResponse<OverviewStats> =
        CommonApiResponse.success(getOverviewStatsUseCase.getOverviewStats(mode))

    @Operation(summary = "플레이어 랭킹 테이블", description = "모드별 플레이어 통계 랭킹을 반환합니다")
    @GetMapping
    fun getStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String
    ): CommonApiResponse<StatsResult> =
        CommonApiResponse.success(getStatsUseCase.getStats(mode))

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

    @Operation(summary = "플레이어 개인 상세 통계")
    @GetMapping("/player/{riotId}")
    fun getPlayerStats(
        @Parameter(description = "플레이어 Riot ID (URL 인코딩)", example = "PlayerName%23KR1")
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "all") mode: String
    ): CommonApiResponse<PlayerDetailStatsResult> =
        CommonApiResponse.success(getPlayerStatsUseCase.getPlayerStats(
            java.net.URLDecoder.decode(riotId, "UTF-8"), mode
        ))
}
