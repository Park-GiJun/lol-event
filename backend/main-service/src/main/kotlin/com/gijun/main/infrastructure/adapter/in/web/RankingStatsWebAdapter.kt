package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.EloLeaderboardResult
import com.gijun.main.application.dto.stats.result.KillParticipationResult
import com.gijun.main.application.dto.stats.result.LaneLeaderboardResult
import com.gijun.main.application.dto.stats.result.MvpStatsResult
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetKillParticipationUseCase
import com.gijun.main.application.port.`in`.GetLaneLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetMvpStatsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Ranking Stats", description = "랭킹/리더보드 통계 API")
@RestController
@RequestMapping("/api/stats")
class RankingStatsWebAdapter(
    private val getEloLeaderboardUseCase: GetEloLeaderboardUseCase,
    private val getMvpStatsUseCase: GetMvpStatsUseCase,
    private val getLaneLeaderboardUseCase: GetLaneLeaderboardUseCase,
    private val getKillParticipationUseCase: GetKillParticipationUseCase,
) {
    @Operation(
        summary = "Elo 리더보드",
        description = "전체 플레이어 Elo 순위를 반환합니다"
    )
    @GetMapping("/elo")
    fun getEloLeaderboard(): CommonApiResponse<EloLeaderboardResult> =
        CommonApiResponse.success(getEloLeaderboardUseCase.getLeaderboard())

    @Operation(
        summary = "MVP 점수 랭킹",
        description = "경기별 MVP 점수(KDA·데미지 기여·시야·CS·승리 보너스 합산)를 집계하여 플레이어 랭킹을 반환합니다"
    )
    @GetMapping("/mvp")
    fun getMvpStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<MvpStatsResult> =
        CommonApiResponse.success(getMvpStatsUseCase.getMvpStats(mode))

    @Operation(
        summary = "라인별 플레이어 랭킹",
        description = "특정 포지션(TOP/JUNGLE/MID/BOTTOM/SUPPORT)에서의 플레이어 통계 랭킹을 반환합니다"
    )
    @GetMapping("/lane")
    fun getLaneLeaderboard(
        @Parameter(description = "포지션 (TOP/JUNGLE/MID/BOTTOM/SUPPORT)", example = "TOP")
        @RequestParam lane: String,
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<LaneLeaderboardResult> =
        CommonApiResponse.success(getLaneLeaderboardUseCase.getLaneLeaderboard(lane, mode))

    @GetMapping("/kill-participation")
    fun getKillParticipation(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<KillParticipationResult> =
        CommonApiResponse.success(getKillParticipationUseCase.getKillParticipation(mode))
}
