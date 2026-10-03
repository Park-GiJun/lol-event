package com.gijun.main.infrastructure.adapter.`in`.web.rag.dto

import com.gijun.main.application.dto.command.AskChatCommand
import com.gijun.main.domain.rag.enums.ChatRole
import com.gijun.main.domain.rag.model.ChatMessageModel
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@Schema(description = "챗봇 질문")
data class AskChatRequest(
    @field:NotEmpty
    @field:Size(max = 40)
    @field:Valid
    @field:Schema(description = "대화 전체. 오래된 것부터, 마지막은 사용자의 질문")
    val messages: List<ChatMessageRequest> = emptyList(),
) {
    fun toCommand() = AskChatCommand(messages = messages.map { ChatMessageModel(it.role, it.content) })
}

data class ChatMessageRequest(
    @field:Schema(description = "누가 한 말인지", example = "USER")
    val role: ChatRole,
    @field:Size(max = 8000)
    @field:Schema(description = "내용", example = "아랑택은 어느 포지션을 주로 가?")
    val content: String,
)
