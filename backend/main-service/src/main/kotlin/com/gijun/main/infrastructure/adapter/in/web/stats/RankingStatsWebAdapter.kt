package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.dto.query.GetLaneLeaderboardQuery
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetKillParticipationUseCase
import com.gijun.main.application.port.`in`.GetLaneLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetMvpStatsUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.rating.dto.EloLeaderboardResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.KillParticipationResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.LaneLeaderboardResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.MvpStatsResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 순위표.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/elo` | Elo 리더보드 |
 * | GET | `/api/stats/mvp` | MVP 점수 랭킹 |
 * | GET | `/api/stats/lane` | 라인별 랭킹 |
 * | GET | `/api/stats/kill-participation` | 킬 관여 랭킹 |
 * ```
 */
@RestController
@RequestMapping("/api/stats", version = "1.0")
@Tag(name = "Ranking Stats", description = "랭킹/리더보드 통계 API")
class RankingStatsWebAdapter(
    private val getEloLeaderboardUseCase: GetEloLeaderboardUseCase,
    private val getMvpStatsUseCase: GetMvpStatsUseCase,
    private val getLaneLeaderboardUseCase: GetLaneLeaderboardUseCase,
    private val getKillParticipationUseCase: GetKillParticipationUseCase,
) {
    @GetMapping("/elo")
    @Operation(
        summary = "Elo 리더보드",
        description =
            "실력 레이팅(laneElo) 표시값 기준 순위를 반환합니다. " +
                "minDuels 미만은 배치 중(rank=0)으로 분류돼 목록 뒤로 밀립니다. " +
                "기준이 경기 수가 아니라 **라인 맞대결 수** 인 점에 주의하세요. 포지션이 깨진 경기나 " +
                "칼바람은 맞대결이 성립하지 않아 실력 표본이 되지 못합니다.",
    )
    fun getEloLeaderboard(
        @Parameter(description = "순위에 들어가기 위한 최소 라인 맞대결 수", example = "10")
        @RequestParam(defaultValue = "10") minDuels: Int,
    ): CommonApiResponse<EloLeaderboardResponse> =
        CommonApiResponse.success(EloLeaderboardResponse.from(getEloLeaderboardUseCase.getEloLeaderboard(minDuels)))

    @GetMapping("/mvp")
    @Operation(
        summary = "MVP 점수 랭킹",
        description = "경기별 MVP 점수(KDA·데미지 기여·시야·CS·승리 보너스 합산)를 집계하여 플레이어 랭킹을 반환합니다",
    )
    fun getMvpStats(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<MvpStatsResponse> = CommonApiResponse.success(MvpStatsResponse.from(getMvpStatsUseCase.getMvpStats(mode)))

    @GetMapping("/lane")
    @Operation(
        summary = "라인별 플레이어 랭킹",
        description = "특정 포지션(TOP/JUNGLE/MID/BOTTOM/SUPPORT)에서의 플레이어 통계 랭킹을 반환합니다",
    )
    fun getLaneLeaderboard(
        @Parameter(description = "포지션 (TOP/JUNGLE/MID/BOTTOM/SUPPORT)", example = "TOP")
        @RequestParam lane: String,
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<LaneLeaderboardResponse> =
        CommonApiResponse.success(
            LaneLeaderboardResponse.from(getLaneLeaderboardUseCase.getLaneLeaderboard(GetLaneLeaderboardQuery(lane, mode))),
        )

    @GetMapping("/kill-participation")
    @Operation(summary = "킬 관여 랭킹")
    fun getKillParticipation(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<KillParticipationResponse> =
        CommonApiResponse.success(KillParticipationResponse.from(getKillParticipationUseCase.getKillParticipation(mode)))
}
