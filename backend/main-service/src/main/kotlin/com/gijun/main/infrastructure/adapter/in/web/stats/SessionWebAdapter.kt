package com.gijun.main.infrastructure.adapter.`in`.web.stats

import com.gijun.main.application.dto.query.GetSessionDetailQuery
import com.gijun.main.application.port.`in`.GetSessionDetailUseCase
import com.gijun.main.application.port.`in`.GetSessionReportUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.session.exception.SessionNotFoundException
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.SessionDetailResponse
import com.gijun.main.infrastructure.adapter.`in`.web.stats.dto.SessionReportResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 세션(하루치 내전).
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/stats/sessions` | 세션 목록 |
 * | GET | `/api/stats/sessions/{date}` | 세션 상세 |
 * ```
 */
@RestController
@RequestMapping("/api/stats/sessions", version = "1.0")
@Tag(name = "Session", description = "세션(하루치 내전) API")
class SessionWebAdapter(
    private val getSessionReportUseCase: GetSessionReportUseCase,
    private val getSessionDetailUseCase: GetSessionDetailUseCase,
) {
    @GetMapping
    @Operation(summary = "세션 목록")
    fun getSessionReport(
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<SessionReportResponse> =
        CommonApiResponse.success(SessionReportResponse.from(getSessionReportUseCase.getSessionReport(mode)))

    @GetMapping("/{date}")
    @Operation(
        summary = "세션 상세 조회",
        description =
            "하루치 내전을 경기 목록·사람별 집계·한타 수까지 반환합니다. " +
                "세션은 오전 6시에 시작하는 하루입니다 — 새벽 경기는 전날 세션에 들어갑니다. " +
                "그 날짜에 경기가 없으면 404, 날짜 형식이 틀리면 400 입니다.",
    )
    fun getSessionDetail(
        @Parameter(description = "세션 날짜 (yyyy-MM-dd)", example = "2026-09-14")
        @PathVariable date: String,
        @RequestParam(defaultValue = "normal") mode: GameMode,
    ): CommonApiResponse<SessionDetailResponse> =
        CommonApiResponse.success(
            SessionDetailResponse.from(
                getSessionDetailUseCase.getSessionDetail(GetSessionDetailQuery(date, mode))
                    ?: throw SessionNotFoundException(date),
            ),
        )
}
