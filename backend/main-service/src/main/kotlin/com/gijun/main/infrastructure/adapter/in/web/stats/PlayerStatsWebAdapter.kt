package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.dto.query.GetEloHistoryQuery
import com.gijun.main.application.dto.query.GetGrowthCurveQuery
import com.gijun.main.application.dto.query.GetPlayerComparisonQuery
import com.gijun.main.application.dto.query.GetPlayerStatsQuery
import com.gijun.main.application.dto.query.GetPlayerStreakQuery
import com.gijun.main.application.port.`in`.GetEloHistoryUseCase
import com.gijun.main.application.port.`in`.GetGrowthCurveUseCase
import com.gijun.main.application.port.`in`.GetPlayerComparisonUseCase
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStreakUseCase
import com.gijun.main.application.port.`in`.GetPlayerTimelineUseCase
import com.gijun.main.application.port.`in`.GetStatsUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.rating.dto.PlayerEloHistoryResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.GrowthCurveResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.PlayerComparisonResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.PlayerDetailStatsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.PlayerTimelineResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.StatsResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.StreakResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import com.gijun.main.shared.infrastructure.web.common.riotIdOf
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
 * 플레이어 축 통계.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats` | 플레이어 랭킹 표 |
 * | GET | `/api/stats/player/{riotId}` | 개인 상세 |
 * | GET | `/api/stats/player/{riotId}/streak` | 연승·연패 |
 * | GET | `/api/stats/player/{riotId}/elo-history` | Elo 변동 내역 |
 * | GET | `/api/stats/player/{riotId}/timeline` | 개인 타임라인 지표 |
 * | GET | `/api/stats/player/{riotId}/growth-curve` | 성장 곡선 |
 * | GET | `/api/stats/compare` | 두 사람 비교 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Player Stats", description = "플레이어별 통계 API")
class PlayerStatsWebAdapter(
    private val getStatsUseCase: GetStatsUseCase,
    private val getPlayerStatsUseCase: GetPlayerStatsUseCase,
    private val getPlayerStreakUseCase: GetPlayerStreakUseCase,
    private val getEloHistoryUseCase: GetEloHistoryUseCase,
    private val getPlayerTimelineUseCase: GetPlayerTimelineUseCase,
    private val getGrowthCurveUseCase: GetGrowthCurveUseCase,
    private val getPlayerComparisonUseCase: GetPlayerComparisonUseCase,
) {
    @GetMapping
    @Operation(summary = "플레이어 랭킹 테이블", description = "모드별 플레이어 통계 랭킹을 반환합니다")
    fun getStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<StatsResponse> = CommonApiResponse.success(StatsResponse.from(getStatsUseCase.getStats(mode)))

    @GetMapping("/player/{riotId}")
    @Operation(summary = "플레이어 개인 상세 통계")
    fun getPlayerStats(
        @Parameter(description = "플레이어 Riot ID (URL 인코딩)", example = "PlayerName%23KR1")
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "all") mode: GameMode,
        @Parameter(description = "포지션 필터 (TOP/JUNGLE/MID/BOTTOM/SUPPORT), 미입력 시 전체")
        @RequestParam(required = false) lane: String? = null,
    ): CommonApiResponse<PlayerDetailStatsResponse> =
        CommonApiResponse.success(
            PlayerDetailStatsResponse.from(
                getPlayerStatsUseCase.getPlayerStats(GetPlayerStatsQuery(riotIdOf(urlDecoded(riotId)), mode, lane)),
            ),
        )

    @GetMapping("/player/{riotId}/streak")
    @Operation(
        summary = "플레이어 연승/연패 기록",
        description = "플레이어의 현재 연승/연패, 역대 최장 연승/연패, 최근 10경기 폼을 반환합니다",
    )
    fun getPlayerStreak(
        @Parameter(description = "플레이어 Riot ID (URL 인코딩)", example = "PlayerName%23KR1")
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "all") mode: GameMode,
    ): CommonApiResponse<StreakResponse> =
        CommonApiResponse.success(
            StreakResponse.from(
                getPlayerStreakUseCase.getPlayerStreak(GetPlayerStreakQuery(riotIdOf(urlDecoded(riotId)), mode)),
            ),
        )

    @GetMapping("/player/{riotId}/elo-history")
    @Operation(summary = "플레이어 Elo 변동 내역", description = "최근 N개 경기의 Elo 변동 히스토리를 반환합니다")
    fun getEloHistory(
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "30") limit: Int,
    ): CommonApiResponse<PlayerEloHistoryResponse> =
        CommonApiResponse.success(
            PlayerEloHistoryResponse.from(
                getEloHistoryUseCase.getEloHistory(GetEloHistoryQuery(riotIdOf(urlDecoded(riotId)), limit)),
            ),
        )

    @GetMapping("/player/{riotId}/timeline")
    @Operation(
        summary = "플레이어 타임라인 지표",
        description = "타임라인이 있는 경기에서의 15분 라인 격차, 초반 킬·데스, 분별 골드 격차 곡선을 반환합니다",
    )
    fun getPlayerTimeline(
        @PathVariable riotId: String,
    ): CommonApiResponse<PlayerTimelineResponse> =
        CommonApiResponse.success(
            PlayerTimelineResponse.from(getPlayerTimelineUseCase.getPlayerTimeline(riotIdOf(urlDecoded(riotId)))),
        )

    @GetMapping("/player/{riotId}/growth-curve")
    @Operation(summary = "플레이어 성장 곡선", description = "경기 순서대로 본 KDA·승률의 이동 평균")
    fun getGrowthCurve(
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "all") mode: GameMode,
    ): CommonApiResponse<GrowthCurveResponse> =
        CommonApiResponse.success(
            GrowthCurveResponse.from(
                getGrowthCurveUseCase.getGrowthCurve(GetGrowthCurveQuery(riotIdOf(urlDecoded(riotId)), mode)),
            ),
        )

    @GetMapping("/compare")
    @Operation(summary = "두 플레이어 비교", description = "같은 팀이었을 때와 상대였을 때의 전적, 그리고 각자의 전체 지표")
    fun getPlayerComparison(
        @RequestParam player1: String,
        @RequestParam player2: String,
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<PlayerComparisonResponse> =
        CommonApiResponse.success(
            PlayerComparisonResponse.from(
                getPlayerComparisonUseCase.getPlayerComparison(
                    GetPlayerComparisonQuery(
                        player1 = riotIdOf(urlDecoded(player1)),
                        player2 = riotIdOf(urlDecoded(player2)),
                        mode = mode,
                    ),
                ),
            ),
        )
}
