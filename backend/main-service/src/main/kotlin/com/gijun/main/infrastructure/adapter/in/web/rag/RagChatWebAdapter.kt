package com.gijun.main.infrastructure.adapter.`in`.web.rag

import com.gijun.main.application.port.`in`.AskChatUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.rag.dto.AskChatRequest
import com.gijun.main.infrastructure.adapter.`in`.web.rag.dto.ChatAnswerResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 챗봇.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | POST | `/api/rag/chat` | 대화의 마지막 질문에 답한다 |
 * ```
 */
@RestController
@RequestMapping("/api/rag", version = "1.0")
@Tag(name = "RAG", description = "내전 기록 질의응답")
class RagChatWebAdapter(
    private val askChatUseCase: AskChatUseCase,
) {
    @PostMapping("/chat")
    @Operation(
        summary = "질문하기",
        description =
            "대화 전체를 보내면 마지막 질문에 답합니다. 서버는 대화를 기억하지 않습니다.\n\n" +
                "답 하나에 수십 초가 걸릴 수 있습니다. LLM 장비가 꺼져 있으면 409 RAG_DISABLED, 줄이 길면 409 RAG_BUSY 입니다.",
    )
    fun askChat(
        @Valid @RequestBody request: AskChatRequest,
    ): CommonApiResponse<ChatAnswerResponse> =
        CommonApiResponse.success(ChatAnswerResponse.from(askChatUseCase.askChat(request.toCommand())))
}
