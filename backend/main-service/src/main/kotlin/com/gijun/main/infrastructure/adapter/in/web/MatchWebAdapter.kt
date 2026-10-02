package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.main.application.dto.command.SaveMatchesCommand
import com.gijun.main.application.dto.result.MatchPageResult
import com.gijun.main.application.dto.result.MatchTimelineResult
import com.gijun.main.application.dto.result.SaveMatchesResult
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.GetMatchTimelineUseCase
import com.gijun.main.application.port.`in`.GetMatchesUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import com.gijun.main.domain.match.exception.MatchNotFoundException
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Match", description = "내전 경기 데이터 API")
@RestController
@RequestMapping("/api/matches")
class MatchWebAdapter(
    private val saveMatchesUseCase: SaveMatchesUseCase,
    private val getMatchesUseCase: GetMatchesUseCase,
    private val getMatchTimelineUseCase: GetMatchTimelineUseCase,
    private val deleteMatchUseCase: DeleteMatchUseCase,
) {
    @Operation(
        summary = "경기 목록 조회 (전체)",
        description = "모드별 경기 전체를 상세 필드까지 반환합니다. 응답이 매우 크므로 목록 화면에서는 /page 를 쓰세요.",
    )
    @Deprecated("목록 화면은 GET /api/matches/page 를 사용한다. 전체 상세가 정말 필요한 배치성 호출만 남긴다.")
    @GetMapping
    fun getAll(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String,
    ) = CommonApiResponse.success(getMatchesUseCase.getAll(mode))

    @Operation(
        summary = "경기 목록 조회 (페이지)",
        description = "최신순 한 페이지를 목록 화면에 필요한 필드만 담아 반환합니다.",
    )
    @GetMapping("/page")
    fun getPage(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: String,
        @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지당 경기 수 (최대 100)", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
    ): CommonApiResponse<MatchPageResult> = CommonApiResponse.success(getMatchesUseCase.getPage(mode, page, size))

    @Operation(summary = "경기 일괄 저장", description = "LCU에서 수집한 경기 데이터를 저장합니다 (upsert)")
    @PostMapping("/bulk")
    fun saveBulk(
        @RequestBody command: SaveMatchesCommand,
    ): CommonApiResponse<SaveMatchesResult> = CommonApiResponse.success(saveMatchesUseCase.save(command))

    @Operation(summary = "경기 단건 조회", description = "matchId로 경기 상세 데이터를 반환합니다")
    @GetMapping("/{matchId}")
    fun getById(
        @Parameter(description = "조회할 경기 ID", example = "KR_8126722699")
        @PathVariable matchId: String,
    ): CommonApiResponse<com.gijun.main.application.dto.result.MatchResult> =
        CommonApiResponse.success(
            getMatchesUseCase.getById(matchId)
                ?: throw MatchNotFoundException(matchId),
        )

    @Operation(
        summary = "경기 단건 타임라인 조회",
        description =
            "골드 곡선, 킬·오브젝트 좌표, 교전 구간을 반환합니다. " +
                "타임라인이 없는 경기는 404 가 아니라 hasTimeline=false 인 빈 결과를 돌려줍니다 — " +
                "타임라인은 새 수집기로 받은 경기에만 있고 이전 경기는 영구히 없습니다(백필 불가). " +
                "경기 자체가 없을 때만 404 입니다.",
    )
    @GetMapping("/{matchId}/timeline")
    fun getTimeline(
        @Parameter(description = "조회할 경기 ID", example = "KR_8126722699")
        @PathVariable matchId: String,
    ): CommonApiResponse<MatchTimelineResult> =
        CommonApiResponse.success(
            getMatchTimelineUseCase.getMatchTimeline(matchId)
                ?: throw MatchNotFoundException(matchId),
        )

    @Operation(summary = "경기 삭제", description = "matchId로 경기 데이터를 삭제합니다")
    @DeleteMapping("/{matchId}")
    fun delete(
        @Parameter(description = "삭제할 경기 ID", example = "KR_8126722699")
        @PathVariable matchId: String,
    ): CommonApiResponse<Unit> {
        deleteMatchUseCase.delete(matchId)
        return CommonApiResponse.success(Unit)
    }
}
