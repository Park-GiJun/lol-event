package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionMatchupResult
import com.gijun.main.application.dto.stats.result.ChampionSynergyResult
import com.gijun.main.application.dto.stats.result.DuoStatsResult
import com.gijun.main.application.dto.stats.result.EloLeaderboardResult
import com.gijun.main.application.dto.stats.result.PlayerEloHistoryResult
import com.gijun.main.application.dto.stats.result.LaneLeaderboardResult
import com.gijun.main.application.dto.stats.result.MvpStatsResult
import com.gijun.main.application.dto.stats.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.StatsResult
import com.gijun.main.application.dto.stats.result.StreakResult
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetChampionSynergyUseCase
import com.gijun.main.application.port.`in`.GetDuoStatsUseCase
import com.gijun.main.application.port.`in`.GetEloHistoryUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetLaneLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetMvpStatsUseCase
import com.gijun.main.application.port.`in`.GetObjectiveCorrelationUseCase
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStreakUseCase
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
    private val getChampionSynergyUseCase: GetChampionSynergyUseCase,
    private val getDuoStatsUseCase: GetDuoStatsUseCase,
    private val getPlayerStreakUseCase: GetPlayerStreakUseCase,
    private val getMvpStatsUseCase: GetMvpStatsUseCase,
    private val getLaneLeaderboardUseCase: GetLaneLeaderboardUseCase,
    private val getChampionMatchupUseCase: GetChampionMatchupUseCase,
    private val getObjectiveCorrelationUseCase: GetObjectiveCorrelationUseCase,
    private val getEloLeaderboardUseCase: GetEloLeaderboardUseCase,
    private val getEloHistoryUseCase: GetEloHistoryUseCase,
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

    @Operation(summary = "플레이어 Elo 변동 내역", description = "최근 N개 경기의 Elo 변동 히스토리를 반환합니다")
    @GetMapping("/player/{riotId}/elo-history")
    fun getEloHistory(
        @PathVariable riotId: String,
        @RequestParam(defaultValue = "30") limit: Int,
    ): CommonApiResponse<PlayerEloHistoryResult> =
        CommonApiResponse.success(getEloHistoryUseCase.getHistory(
            java.net.URLDecoder.decode(riotId, "UTF-8"), limit
        ))

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
        summary = "챔피언 조합 시너지 분석",
        description = "같은 팀에서 함께 플레이한 챔피언 조합의 승률을 반환합니다"
    )
    @GetMapping("/synergy")
    fun getChampionSynergy(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String,
        @Parameter(description = "최소 게임 수 필터", example = "3")
        @RequestParam(defaultValue = "3") minGames: Int,
    ): CommonApiResponse<ChampionSynergyResult> =
        CommonApiResponse.success(getChampionSynergyUseCase.getChampionSynergy(mode, minGames))

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
        summary = "챔피언 상성 매트릭스",
        description = "champion=X: X의 각 상대 챔피언 vs 승률 / vsChampion=X: X를 상대하는 챔피언 중 카운터픽 추천"
    )
    @GetMapping("/matchup")
    fun getMatchup(
        @RequestParam(required = false) champion: String?,
        @RequestParam(required = false) vsChampion: String?,
        @RequestParam(defaultValue = "normal") mode: String,
        @RequestParam(defaultValue = "false") samePosition: Boolean,
    ): CommonApiResponse<ChampionMatchupResult> =
        CommonApiResponse.success(getChampionMatchupUseCase.getMatchup(
            champion?.let { java.net.URLDecoder.decode(it, "UTF-8") },
            vsChampion?.let { java.net.URLDecoder.decode(it, "UTF-8") },
            mode,
            samePosition,
        ))

    @Operation(
        summary = "오브젝트 선점 → 승률 상관관계",
        description = "퍼블/드래곤/바론/포탑 선점팀의 승률과 미선점팀 승률 비교"
    )
    @GetMapping("/objectives")
    fun getObjectiveCorrelation(
        @RequestParam(defaultValue = "normal") mode: String,
    ): CommonApiResponse<ObjectiveCorrelationResult> =
        CommonApiResponse.success(getObjectiveCorrelationUseCase.getObjectiveCorrelation(mode))

    @Operation(
        summary = "Elo 리더보드",
        description = "전체 플레이어 Elo 순위를 반환합니다"
    )
    @GetMapping("/elo")
    fun getEloLeaderboard(): CommonApiResponse<EloLeaderboardResult> =
        CommonApiResponse.success(getEloLeaderboardUseCase.getLeaderboard())

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
}
