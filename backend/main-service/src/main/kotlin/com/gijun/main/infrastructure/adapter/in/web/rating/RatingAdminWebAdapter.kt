package com.gijun.main.infrastructure.adapter.`in`.web.rating

import com.gijun.main.application.dto.query.ValidateRatingQuery
import com.gijun.main.application.port.`in`.GetRatingUseCase
import com.gijun.main.application.port.`in`.GetRatingsUseCase
import com.gijun.main.application.port.`in`.ResetAndRecalculateRatingUseCase
import com.gijun.main.application.port.`in`.ValidateRatingUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.rating.dto.PlayerRatingResponse
import com.gijun.main.infrastructure.adapter.`in`.web.rating.dto.RatingValidationResponse
import com.gijun.main.infrastructure.adapter.`in`.web.rating.dto.RecalculateResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import com.gijun.main.shared.infrastructure.web.common.riotIdOf
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 레이팅 관리 — 관리자 전용.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/admin/elo` | 전원의 두 레이팅 원값 |
 * | GET | `/api/admin/elo/{riotId}` | 한 사람의 레이팅 |
 * | GET | `/api/admin/elo/validate` | 워크포워드 검증 |
 * | POST | `/api/admin/elo/reset` | 전체 초기화 후 재집계 |
 * ```
 * 화면이 보는 리더보드(`/api/stats/elo`)는 표시값(수축 적용) 기준이고, 여기는 **원값**이다.
 */
@RestController
@RequestMapping("/api/admin/elo", version = "1.0")
@Tag(name = "Admin", description = "관리자 전용 API")
class RatingAdminWebAdapter(
    private val getRatingsUseCase: GetRatingsUseCase,
    private val getRatingUseCase: GetRatingUseCase,
    private val validateRatingUseCase: ValidateRatingUseCase,
    private val resetAndRecalculateRatingUseCase: ResetAndRecalculateRatingUseCase,
) {
    // ===== 조회 =====

    @GetMapping
    @Operation(summary = "전체 레이팅 조회", description = "모든 플레이어의 laneElo / teamElo 원값을 반환합니다.")
    fun getRatings(): CommonApiResponse<List<PlayerRatingResponse>> =
        CommonApiResponse.success(getRatingsUseCase.getRatings().map(PlayerRatingResponse::from))

    @GetMapping("/{riotId}")
    @Operation(summary = "플레이어 레이팅 조회", description = "기록이 없는 사람은 시작 점수로 돌려줍니다.")
    fun getRating(
        @PathVariable riotId: String,
    ): CommonApiResponse<PlayerRatingResponse> =
        CommonApiResponse.success(PlayerRatingResponse.from(getRatingUseCase.getRating(riotIdOf(riotId))))

    @GetMapping("/validate")
    @Operation(
        summary = "레이팅 워크포워드 검증",
        description =
            "저장된 레이팅을 건드리지 않고 메모리에서 처음부터 재생하면서, 매 경기를 " +
                "그 시점 레이팅으로만 예측해 로그로스·적중률을 냅니다.\n\n" +
                "세션(6시간 간격) 첫 경기만 모은 값이 더 정직합니다 — 세션 안에서는 편성이 반복돼 누수가 섞입니다.\n" +
                "excludeRepeatedTeams 를 끄면 K 가 클수록 좋아 보이는 착시가 생기므로 기본값을 유지하는 것을 권합니다.",
    )
    fun validateRating(
        @Parameter(description = "예열 경기 수. 이 경기들은 레이팅을 움직이지만 평가에는 들어가지 않습니다", example = "30")
        @RequestParam(defaultValue = "30") warmup: Int,
        @Parameter(description = "직전 경기와 팀 구성이 같은(진영만 바뀐 경우 포함) 경기를 평가에서 제외")
        @RequestParam(defaultValue = "true") excludeRepeatedTeams: Boolean,
    ): CommonApiResponse<RatingValidationResponse> =
        CommonApiResponse.success(
            RatingValidationResponse.from(validateRatingUseCase.validateRating(ValidateRatingQuery(warmup, excludeRepeatedTeams))),
        )

    // ===== 변경 =====

    @PostMapping("/reset")
    @Operation(
        summary = "레이팅 전체 초기화 및 재집계",
        description =
            "laneElo / teamElo 를 모두 초기화하고 전체 매치를 gameCreation 오름차순으로 재집계합니다.\n\n" +
                "경기마다 그 경기에 저장된 데이터로 라인 판정 방법(TIMELINE_15 / LEGACY_FINAL)이 자동으로 정해지므로 " +
                "과거 경기와 신규 경기가 한 레이팅 안에 섞여도 됩니다. Elo 는 매 경기 내부에서만 비교하기 때문입니다.",
    )
    fun resetAndRecalculateRating(): CommonApiResponse<RecalculateResponse> =
        CommonApiResponse.success(RecalculateResponse.from(resetAndRecalculateRatingUseCase.resetAndRecalculateRating()))
}
