package com.gijun.main.infrastructure.adapter.`in`.web.match

import com.gijun.main.application.port.`in`.ReassignPositionsUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.match.dto.ReassignPositionsResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 포지션 백필 — 관리자 전용.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | POST | `/api/admin/positions/reassign` | 저장된 전 경기의 포지션 재배정 |
 * ```
 */
@RestController
@RequestMapping("/api/admin/positions", version = "1.0")
@Tag(name = "Admin", description = "관리자 전용 API")
class MatchPositionAdminWebAdapter(
    private val reassignPositionsUseCase: ReassignPositionsUseCase,
) {
    @PostMapping("/reassign")
    @Operation(
        summary = "포지션 재배정 백필",
        description =
            "저장된 모든 매치를 스캔해 포지션을 재배정합니다. 칼바람은 제외. 완료 후 /elo/reset 권장.\n\n" +
                "force=false(기본): 한 팀에 TOP/JUNGLE/MID/ADC/SUPPORT 가 정확히 하나씩 들어있지 않은 깨진 팀만 손댑니다.\n" +
                "force=true: 모든 팀을 다시 계산합니다. 배정 규칙이 바뀐 뒤에는 반드시 이쪽을 써야 합니다 — " +
                "기존 데이터는 5포지션이 하나씩 채워져 있고 값만 틀린 상태라 기본 모드로는 한 건도 고쳐지지 않습니다.",
    )
    fun reassignPositions(
        @Parameter(description = "true 면 깨지지 않은 팀까지 전부 다시 계산한다")
        @RequestParam(defaultValue = "false") force: Boolean,
    ): CommonApiResponse<ReassignPositionsResponse> =
        CommonApiResponse.success(ReassignPositionsResponse.from(reassignPositionsUseCase.reassignPositions(force)))
}
