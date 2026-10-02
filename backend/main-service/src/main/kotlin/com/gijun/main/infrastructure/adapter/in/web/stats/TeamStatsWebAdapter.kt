package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.dto.query.GetDuoStatsQuery
import com.gijun.main.application.dto.query.GetRivalMatchupQuery
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.`in`.GetRivalMatchupUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.DuoStatsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.RivalMatchupResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 사람 사이의 관계 통계.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/duo` | 같은 팀이었을 때 |
 * | GET | `/api/stats/rival-matchup` | 상대였을 때 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Team Stats", description = "팀/듀오/라이벌 통계 API")
class TeamStatsWebAdapter(
    private val getDuoStatsUseCase: GetDuoStatsUseCase,
    private val getRivalMatchupUseCase: GetRivalMatchupUseCase,
) {
    @GetMapping("/duo")
    @Operation(
        summary = "플레이어 듀오 시너지 분석",
        description = "같은 팀에서 함께 플레이한 플레이어 조합의 승률과 통계를 반환합니다",
    )
    fun getDuoStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: GameMode,
        @Parameter(description = "최소 게임 수 필터", example = "2")
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<DuoStatsResponse> =
        CommonApiResponse.success(DuoStatsResponse.from(getDuoStatsUseCase.getDuoStats(GetDuoStatsQuery(mode, minGames))))

    @GetMapping("/rival-matchup")
    @Operation(summary = "라이벌 전적", description = "서로 다른 팀으로 맞붙었을 때의 상대 전적")
    fun getRivalMatchup(
        @RequestParam(defaultValue = "normal") mode: GameMode,
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<RivalMatchupResponse> =
        CommonApiResponse.success(
            RivalMatchupResponse.from(getRivalMatchupUseCase.getRivalMatchup(GetRivalMatchupQuery(mode, minGames))),
        )
}
