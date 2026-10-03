package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.AskChatCommand
import com.gijun.main.application.dto.result.ChatAnswerResult
import com.gijun.main.application.port.`in`.AskChatUseCase
import com.gijun.main.application.port.out.external.LlmChatPort
import com.gijun.main.domain.rag.enums.ChatRole
import com.gijun.main.domain.rag.exception.InvalidChatRequestException
import org.springframework.stereotype.Service

/**
 * 챗봇 질문 하나.
 *
 * 대화는 서버가 기억하지 않는다 — 화면이 매번 지난 대화를 같이 보낸다. 그래서 여기서 길이를 자른다.
 * 모델의 컨텍스트(32k)는 tool 결과가 대부분을 쓰므로 대화에 줄 몫은 크지 않다.
 */
@Service
class RagChatCommandHandler(
    private val llmChatPort: LlmChatPort,
) : AskChatUseCase {
    override fun askChat(command: AskChatCommand): ChatAnswerResult {
        val messages = command.messages.filter { it.content.isNotBlank() }
        val last = messages.lastOrNull() ?: throw InvalidChatRequestException("질문이 비어 있습니다")
        if (last.role != ChatRole.USER) throw InvalidChatRequestException("마지막 메시지는 질문이어야 합니다")
        if (last.content.length > MAX_QUESTION_LENGTH) {
            throw InvalidChatRequestException("질문이 너무 깁니다 (${MAX_QUESTION_LENGTH}자까지)")
        }

        val history =
            messages
                .dropLast(1)
                .takeLast(MAX_HISTORY_MESSAGES)
                .map { it.copy(content = it.content.take(MAX_HISTORY_MESSAGE_LENGTH)) }

        return ChatAnswerResult(llmChatPort.answer(history, last.content.trim()).trim())
    }

    private companion object {
        const val MAX_QUESTION_LENGTH = 1_000
        const val MAX_HISTORY_MESSAGES = 8
        const val MAX_HISTORY_MESSAGE_LENGTH = 1_500
    }
}
