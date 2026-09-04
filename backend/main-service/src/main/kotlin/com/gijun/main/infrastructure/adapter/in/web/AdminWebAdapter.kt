package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.port.`in`.GetEloUseCase
import com.gijun.main.application.port.`in`.ReassignPositionsResult
import com.gijun.main.application.port.`in`.ReassignPositionsUseCase
import com.gijun.main.application.port.`in`.ResetAndRecalculateEloUseCase
import com.gijun.main.domain.model.elo.PlayerElo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin", description = "관리자 전용 API")
@RestController
@RequestMapping("/api/admin")
class AdminWebAdapter(
    private val resetAndRecalculateEloUseCase: ResetAndRecalculateEloUseCase,
    private val getEloUseCase: GetEloUseCase,
    private val reassignPositionsUseCase: ReassignPositionsUseCase,
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
    @Operation(summary = "Elo 전체 초기화 및 재집계", description = "모든 Elo 데이터를 초기화하고 전체 매치를 시간순으로 재집계합니다.")
    @PostMapping("/elo/reset")
    fun resetElo(): CommonApiResponse<String> {
        resetAndRecalculateEloUseCase.resetAndRecalculate()
        return CommonApiResponse.success("Elo 전체 재집계 완료")
    }

    @Operation(summary = "전체 Elo 조회", description = "모든 플레이어의 현재 Elo를 반환합니다.")
    @GetMapping("/elo")
    fun getAllElo(): CommonApiResponse<List<PlayerElo>> =
        CommonApiResponse.success(
            getEloUseCase.getAll().sortedByDescending { it.elo }
        )

    @Operation(summary = "플레이어 Elo 조회")
    @GetMapping("/elo/{riotId}")
    fun getElo(@PathVariable riotId: String): CommonApiResponse<PlayerElo> {
        val elo = getEloUseCase.getByRiotId(riotId)
            ?: return CommonApiResponse.success(
                com.gijun.main.domain.model.elo.PlayerElo(riotId = riotId)
            )
        return CommonApiResponse.success(elo)
    }
}
