package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.stats.result.RatingValidationResult
import com.gijun.main.application.dto.stats.result.RecalculateResult
import com.gijun.main.application.port.`in`.GetRatingUseCase
import com.gijun.main.application.port.`in`.ReassignPositionsResult
import com.gijun.main.application.port.`in`.ReassignPositionsUseCase
import com.gijun.main.application.port.`in`.ResetAndRecalculateRatingUseCase
import com.gijun.main.application.port.`in`.ValidateRatingUseCase
import com.gijun.main.domain.model.rating.PlayerRating
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin", description = "관리자 전용 API")
@RestController
@RequestMapping("/api/admin")
class AdminWebAdapter(
    private val resetAndRecalculateRatingUseCase: ResetAndRecalculateRatingUseCase,
    private val getRatingUseCase: GetRatingUseCase,
    private val reassignPositionsUseCase: ReassignPositionsUseCase,
    private val validateRatingUseCase: ValidateRatingUseCase,
) {

    @Operation(
        summary = "포지션 재배정 백필",
        description = "저장된 모든 매치를 스캔해 포지션을 재배정합니다. 칼바람은 제외. 완료 후 /elo/reset 권장.\n\n" +
            "force=false(기본): 한 팀에 TOP/JUNGLE/MID/ADC/SUPPORT 가 정확히 하나씩 들어있지 않은 깨진 팀만 손댑니다.\n" +
            "force=true: 모든 팀을 다시 계산합니다. 배정 규칙이 바뀐 뒤에는 반드시 이쪽을 써야 합니다 — " +
            "기존 데이터는 5포지션이 하나씩 채워져 있고 값만 틀린 상태라 기본 모드로는 한 건도 고쳐지지 않습니다."
    )
    @PostMapping("/positions/reassign")
    fun reassignPositions(
        @RequestParam(defaultValue = "false") force: Boolean,
    ): CommonApiResponse<ReassignPositionsResult> =
        CommonApiResponse.success(reassignPositionsUseCase.reassignAll(force))

    @Operation(
        summary = "레이팅 전체 초기화 및 재집계",
        description = "laneElo / teamElo 를 모두 초기화하고 전체 매치를 gameCreation 오름차순으로 재집계합니다.\n\n" +
            "경기마다 그 경기에 저장된 데이터로 라인 판정 방법(TIMELINE_15 / LEGACY_FINAL)이 자동으로 정해지므로 " +
            "과거 경기와 신규 경기가 한 레이팅 안에 섞여도 됩니다. Elo 는 매 경기 내부에서만 비교하기 때문입니다."
    )
    @PostMapping("/elo/reset")
    fun resetRatings(): CommonApiResponse<RecalculateResult> =
        CommonApiResponse.success(resetAndRecalculateRatingUseCase.resetAndRecalculate())

    @Operation(
        summary = "레이팅 워크포워드 검증",
        description = "저장된 레이팅을 건드리지 않고 메모리에서 처음부터 재생하면서, 매 경기를 " +
            "그 시점 레이팅으로만 예측해 로그로스·적중률을 냅니다.\n\n" +
            "세션(6시간 간격) 첫 경기만 모은 값이 더 정직합니다 — 세션 안에서는 편성이 반복돼 누수가 섞입니다.\n" +
            "excludeRepeatedTeams 를 끄면 K 가 클수록 좋아 보이는 착시가 생기므로 기본값을 유지하는 것을 권합니다."
    )
    @GetMapping("/elo/validate")
    fun validate(
        @Parameter(description = "예열 경기 수. 이 경기들은 레이팅을 움직이지만 평가에는 들어가지 않습니다", example = "30")
        @RequestParam(defaultValue = "30") warmup: Int,
        @Parameter(description = "직전 경기와 팀 구성이 같은(진영만 바뀐 경우 포함) 경기를 평가에서 제외")
        @RequestParam(defaultValue = "true") excludeRepeatedTeams: Boolean,
    ): CommonApiResponse<RatingValidationResult> =
        CommonApiResponse.success(validateRatingUseCase.validate(warmup, excludeRepeatedTeams))

    @Operation(summary = "전체 레이팅 조회", description = "모든 플레이어의 laneElo / teamElo 원값을 반환합니다.")
    @GetMapping("/elo")
    fun getAllRatings(): CommonApiResponse<List<PlayerRating>> =
        CommonApiResponse.success(getRatingUseCase.getAll().sortedByDescending { it.laneElo })

    @Operation(summary = "플레이어 레이팅 조회", description = "riotId 는 별명 정규화를 거칩니다.")
    @GetMapping("/elo/{riotId}")
    fun getRating(@PathVariable riotId: String): CommonApiResponse<PlayerRating> =
        CommonApiResponse.success(
            getRatingUseCase.getByRiotId(riotId) ?: PlayerRating(riotId = riotId)
        )
}
