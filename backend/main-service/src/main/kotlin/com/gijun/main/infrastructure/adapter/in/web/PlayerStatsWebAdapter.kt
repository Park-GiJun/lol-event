package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.GrowthCurveResult
import com.gijun.main.application.dto.stats.result.PlayerComparisonResult
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.PlayerEloHistoryResult
import com.gijun.main.application.dto.stats.result.StatsResult
import com.gijun.main.application.dto.stats.result.StreakResult
import com.gijun.main.application.port.`in`.GetEloHistoryUseCase
import com.gijun.main.application.port.`in`.GetGrowthCurveUseCase
import com.gijun.main.application.port.`in`.GetPlayerComparisonUseCase
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStreakUseCase
import com.gijun.main.application.port.`in`.GetStatsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Player Stats", description = "플레이어별 통계 API")
@RestController
@RequestMapping("/api/stats")
class PlayerStatsWebAdapter(
    private val getStatsUseCase: GetStatsUseCase,
    private val getPlayerStatsUseCase: GetPlayerStatsUseCase,
    private val getPlayerStreakUseCase: GetPlayerStreakUseCase,
    private val getEloHistoryUseCase: GetEloHistoryUseCase,
    private val getGrowthCurveUseCase: GetGrowthCurveUseCase,
    private val getPlayerComparisonUseCase: GetPlayerComparisonUseCase,
) {
    @Operation(summary = "플레이어 랭킹 테이블", description = "모드별 플레이어 통계 랭킹을 반환합니다")
    @GetMapping
    fun getStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String
    ): CommonApiResponse<StatsResult> =
        CommonApiResponse.success(getStatsUseCase.getStats(mode))

    @Operation(summary = "플레이어 개인 상세 통계")
    @GetMapping("/player/{riotId}")
    fun getPlayerStats(
        @Parameter(description = "플레이어 Riot ID (URL 인코딩)", example = "PlayerName%23KR1")
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "all") mode: String,
        @Parameter(description = "포지션 필터 (TOP/JUNGLE/MID/BOTTOM/SUPPORT), 미입력 시 전체")
        @RequestParam(required = false) lane: String? = null,
    ): CommonApiResponse<PlayerDetailStatsResult> =
        CommonApiResponse.success(getPlayerStatsUseCase.getPlayerStats(
            java.net.URLDecoder.decode(riotId, "UTF-8"), mode, lane
        ))

    @Operation(
        summary = "플레이어 연승/연패 기록",
        description = "플레이어의 현재 연승/연패, 역대 최장 연승/연패, 최근 10경기 폼을 반환합니다"
    )
    @GetMapping("/player/{riotId}/streak")
    fun getPlayerStreak(
        @Parameter(description = "플레이어 Riot ID (URL 인코딩)", example = "PlayerName%23KR1")
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "all") mode: String,
    ): CommonApiResponse<StreakResult> =
        CommonApiResponse.success(getPlayerStreakUseCase.getPlayerStreak(
            java.net.URLDecoder.decode(riotId, "UTF-8"), mode
        ))

    @Operation(summary = "플레이어 Elo 변동 내역", description = "최근 N개 경기의 Elo 변동 히스토리를 반환합니다")
    @GetMapping("/player/{riotId}/elo-history")
    fun getEloHistory(
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "30") limit: Int,
    ): CommonApiResponse<PlayerEloHistoryResult> =
        CommonApiResponse.success(getEloHistoryUseCase.getHistory(
            java.net.URLDecoder.decode(riotId, "UTF-8"), limit
        ))

    @GetMapping("/player/{riotId}/growth-curve")
    fun getGrowthCurve(
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "all") mode: String,
    ): CommonApiResponse<GrowthCurveResult> =
        CommonApiResponse.success(getGrowthCurveUseCase.getGrowthCurve(
            java.net.URLDecoder.decode(riotId, "UTF-8"), mode
        ))

    @GetMapping("/compare")
    fun getPlayerComparison(
        @RequestParam player1: String,
        @RequestParam player2: String,
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<PlayerComparisonResult> =
        CommonApiResponse.success(getPlayerComparisonUseCase.getPlayerComparison(
            java.net.URLDecoder.decode(player1, "UTF-8"),
            java.net.URLDecoder.decode(player2, "UTF-8"),
            mode,
        ))
}
