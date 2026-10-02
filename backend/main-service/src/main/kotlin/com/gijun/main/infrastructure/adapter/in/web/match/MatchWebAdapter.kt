package com.gijun.main.infrastructure.adapter.`in`.web.match

import com.gijun.main.application.dto.query.GetMatchPageQuery
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.GetMatchPageUseCase
import com.gijun.main.application.port.`in`.GetMatchTimelineUseCase
import com.gijun.main.application.port.`in`.GetMatchUseCase
import com.gijun.main.application.port.`in`.GetMatchesUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.exception.MatchNotFoundException
import com.gijun.main.infrastructure.adapter.`in`.web.match.dto.MatchPageResponse
import com.gijun.main.infrastructure.adapter.`in`.web.match.dto.MatchResponse
import com.gijun.main.infrastructure.adapter.`in`.web.match.dto.MatchTimelineResponse
import com.gijun.main.infrastructure.adapter.`in`.web.match.dto.SaveMatchesRequest
import com.gijun.main.infrastructure.adapter.`in`.web.match.dto.SaveMatchesResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import com.gijun.main.shared.infrastructure.web.common.matchIdOf
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 경기 데이터.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/matches` | 전체 목록(상세 필드까지). 응답이 매우 크다 |
 * | GET | `/api/matches/page` | 최신순 한 페이지(요약 필드) |
 * | GET | `/api/matches/{matchId}` | 단건 |
 * | GET | `/api/matches/{matchId}/timeline` | 단건 타임라인 |
 * | POST | `/api/matches/bulk` | 수집기가 올리는 일괄 저장 |
 * | DELETE | `/api/matches/{matchId}` | 삭제 |
 * ```
 * ⚠️ **`/bulk` 의 경로와 본문 모양은 이미 배포된 데스크탑 수집기가 쓰는 계약이다.** 바꾸면 수집이
 * 조용히 멈춘다.
 */
@RestController
@RequestMapping("/api/matches", version = "1.0")
@Tag(name = "Match", description = "내전 경기 데이터 API")
class MatchWebAdapter(
    private val getMatchesUseCase: GetMatchesUseCase,
    private val getMatchPageUseCase: GetMatchPageUseCase,
    private val getMatchUseCase: GetMatchUseCase,
    private val getMatchTimelineUseCase: GetMatchTimelineUseCase,
    private val saveMatchesUseCase: SaveMatchesUseCase,
    private val deleteMatchUseCase: DeleteMatchUseCase,
) {
    // ===== 조회 =====

    @GetMapping
    @Operation(
        summary = "경기 목록 조회 (전체)",
        description = "모드별 경기 전체를 상세 필드까지 반환합니다. 응답이 매우 크므로 목록 화면에서는 /page 를 쓰세요.",
        deprecated = true,
    )
    fun getMatches(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<List<MatchResponse>> = CommonApiResponse.success(getMatchesUseCase.getMatches(mode).map(MatchResponse::from))

    @GetMapping("/page")
    @Operation(
        summary = "경기 목록 조회 (페이지)",
        description = "최신순 한 페이지를 목록 화면에 필요한 필드만 담아 반환합니다.",
    )
    fun getMatchPage(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "normal")
        @RequestParam(defaultValue = "normal") mode: GameMode,
        @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지당 경기 수 (최대 100)", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
    ): CommonApiResponse<MatchPageResponse> =
        CommonApiResponse.success(MatchPageResponse.from(getMatchPageUseCase.getMatchPage(GetMatchPageQuery(mode, page, size))))

    @GetMapping("/{matchId}")
    @Operation(summary = "경기 단건 조회", description = "matchId로 경기 상세 데이터를 반환합니다")
    fun getMatch(
        @Parameter(description = "조회할 경기 ID", example = "KR_8126722699")
        @PathVariable matchId: String,
    ): CommonApiResponse<MatchResponse> =
        CommonApiResponse.success(
            MatchResponse.from(getMatchUseCase.getMatch(matchIdOf(matchId)) ?: throw MatchNotFoundException(matchId)),
        )

    @GetMapping("/{matchId}/timeline")
    @Operation(
        summary = "경기 단건 타임라인 조회",
        description =
            "골드 곡선, 킬·오브젝트 좌표, 교전 구간을 반환합니다. " +
                "타임라인이 없는 경기는 404 가 아니라 hasTimeline=false 인 빈 결과를 돌려줍니다 — " +
                "타임라인은 새 수집기로 받은 경기에만 있고 이전 경기는 영구히 없습니다(백필 불가). " +
                "경기 자체가 없을 때만 404 입니다.",
    )
    fun getMatchTimeline(
        @Parameter(description = "조회할 경기 ID", example = "KR_8126722699")
        @PathVariable matchId: String,
    ): CommonApiResponse<MatchTimelineResponse> =
        CommonApiResponse.success(
            MatchTimelineResponse.from(
                getMatchTimelineUseCase.getMatchTimeline(matchIdOf(matchId)) ?: throw MatchNotFoundException(matchId),
            ),
        )

    // ===== 변경 =====

    @PostMapping("/bulk")
    @Operation(summary = "경기 일괄 저장", description = "LCU에서 수집한 경기 데이터를 저장합니다. 이미 있는 경기는 건너뜁니다")
    fun saveMatches(
        @Valid @RequestBody request: SaveMatchesRequest,
    ): CommonApiResponse<SaveMatchesResponse> =
        CommonApiResponse.success(SaveMatchesResponse.from(saveMatchesUseCase.saveMatches(request.toCommand())))

    @DeleteMapping("/{matchId}")
    @Operation(summary = "경기 삭제", description = "matchId로 경기 데이터를 삭제합니다")
    fun deleteMatch(
        @Parameter(description = "삭제할 경기 ID", example = "KR_8126722699")
        @PathVariable matchId: String,
    ): CommonApiResponse<Unit> {
        deleteMatchUseCase.deleteMatch(matchIdOf(matchId))
        return CommonApiResponse.success(Unit)
    }
}
