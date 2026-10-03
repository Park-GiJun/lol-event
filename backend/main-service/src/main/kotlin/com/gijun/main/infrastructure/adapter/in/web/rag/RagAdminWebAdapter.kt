package com.gijun.main.infrastructure.adapter.`in`.web.rag

import com.gijun.main.application.port.`in`.GetRagIndexStatusUseCase
import com.gijun.main.application.port.`in`.StartRagReindexUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.rag.dto.RagIndexStatusResponse
import com.gijun.main.infrastructure.adapter.`in`.web.rag.dto.StartRagReindexResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 검색 문서 색인 — 운영자가 손으로 돌리는 입구.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | POST | `/api/admin/rag/reindex` | 전체를 다시 쓴다(뒤에서 돈다) |
 * | GET | `/api/admin/rag/status` | 진행 상황과 저장된 문서 수 |
 * ```
 */
@RestController
@RequestMapping("/api/admin/rag", version = "1.0")
@Tag(name = "Admin", description = "관리자 전용 API")
class RagAdminWebAdapter(
    private val startRagReindexUseCase: StartRagReindexUseCase,
    private val getRagIndexStatusUseCase: GetRagIndexStatusUseCase,
) {
    @GetMapping("/status")
    @Operation(summary = "색인 상태")
    fun getRagIndexStatus(): CommonApiResponse<RagIndexStatusResponse> =
        CommonApiResponse.success(RagIndexStatusResponse.from(getRagIndexStatusUseCase.getRagIndexStatus()))

    @PostMapping("/reindex")
    @Operation(
        summary = "전체 다시 색인",
        description =
            "모든 경기·플레이어·챔피언 문서를 다시 씁니다. 글이 그대로인 문서는 임베딩하지 않으므로 여러 번 눌러도 됩니다.\n\n" +
                "요청은 바로 돌아오고 색인은 뒤에서 돕니다. 이미 돌고 있으면 `started: false` 입니다.",
    )
    fun startRagReindex(): CommonApiResponse<StartRagReindexResponse> =
        CommonApiResponse.success(StartRagReindexResponse.from(startRagReindexUseCase.startRagReindex()))
}
