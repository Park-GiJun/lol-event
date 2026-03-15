package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.StatsResult
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
) {

    @Operation(summary = "플레이어 통계 조회", description = "모드별 플레이어 통계(KDA, 승률, 평균 딜량, 주요 챔피언 등)를 반환합니다")
    @GetMapping
    fun getStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String
    ): CommonApiResponse<StatsResult> =
        CommonApiResponse.success(getStatsUseCase.getStats(mode))

    @Operation(summary = "플레이어 개인 통계 조회")
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
