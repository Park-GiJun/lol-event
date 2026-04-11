package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.DuoStatsResult
import com.gijun.main.application.dto.stats.result.RivalMatchupResult
import com.gijun.main.application.dto.stats.result.TeamChemistryResult
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.`in`.GetRivalMatchupUseCase
import com.gijun.main.application.port.`in`.GetTeamChemistryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Team Stats", description = "팀/듀오/라이벌 통계 API")
@RestController
@RequestMapping("/api/stats")
class TeamStatsWebAdapter(
    private val getDuoStatsUseCase: GetDuoStatsUseCase,
    private val getRivalMatchupUseCase: GetRivalMatchupUseCase,
    private val getTeamChemistryUseCase: GetTeamChemistryUseCase,
) {
    @Operation(
        summary = "플레이어 듀오 시너지 분석",
        description = "같은 팀에서 함께 플레이한 플레이어 조합의 승률과 통계를 반환합니다"
    )
    @GetMapping("/duo")
    fun getDuoStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String,
        @Parameter(description = "최소 게임 수 필터", example = "2")
        @RequestParam(defaultValue = "2") minGames: Int,
    ): CommonApiResponse<DuoStatsResult> =
        CommonApiResponse.success(getDuoStatsUseCase.getDuoStats(mode, minGames))

    @GetMapping("/rival-matchup")
    fun getRivalMatchup(
        @RequestParam(defaultValue = "normal") mode: String,
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<RivalMatchupResult> =
        CommonApiResponse.success(getRivalMatchupUseCase.getRivalMatchups(mode, minGames))

    @GetMapping("/team-chemistry")
    fun getTeamChemistry(
        @RequestParam(defaultValue = "normal") mode: String,
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<TeamChemistryResult> =
        CommonApiResponse.success(getTeamChemistryUseCase.getTeamChemistry(mode, minGames))
}
