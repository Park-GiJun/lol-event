package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.match.command.SaveMatchesCommand
import com.gijun.main.application.dto.match.result.SaveMatchesResult
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.GetMatchesUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Match", description = "내전 경기 데이터 API")
@RestController
@RequestMapping("/api/matches")
class MatchWebAdapter(
    private val saveMatchesUseCase: SaveMatchesUseCase,
    private val getMatchesUseCase: GetMatchesUseCase,
    private val deleteMatchUseCase: DeleteMatchUseCase
) {
    @Operation(summary = "경기 목록 조회", description = "모드(normal/aram/all)별 경기 목록을 반환합니다")
    @GetMapping
    fun getAll(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String
    ) = CommonApiResponse.success(getMatchesUseCase.getAll(mode))

    @Operation(summary = "경기 일괄 저장", description = "LCU에서 수집한 경기 데이터를 저장합니다 (upsert)")
    @PostMapping("/bulk")
    fun saveBulk(@RequestBody command: SaveMatchesCommand): CommonApiResponse<SaveMatchesResult> =
        CommonApiResponse.success(saveMatchesUseCase.save(command))

    @Operation(summary = "경기 삭제", description = "matchId로 경기 데이터를 삭제합니다")
    @DeleteMapping("/{matchId}")
    fun delete(
        @Parameter(description = "삭제할 경기 ID", example = "KR_8126722699")
        @PathVariable matchId: String
    ): CommonApiResponse<Unit> {
        deleteMatchUseCase.delete(matchId)
        return CommonApiResponse.success(Unit)
    }
}
