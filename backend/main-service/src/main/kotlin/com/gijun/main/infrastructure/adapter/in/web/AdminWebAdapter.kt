package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.port.`in`.GetEloUseCase
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
) {
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
