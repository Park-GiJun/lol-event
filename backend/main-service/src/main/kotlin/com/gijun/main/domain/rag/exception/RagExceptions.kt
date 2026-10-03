package com.gijun.main.domain.rag.exception

import com.gijun.main.shared.domain.exception.ConflictException
import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.ValidationException

/** LLM 장비가 꺼져 있거나 답이 오지 않았다. 장비가 돌아오면 풀린다. */
class RagUnavailableException(
    cause: Throwable? = null,
) : ConflictException("AI 서버에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요", ErrorCode.RAG_UNAVAILABLE) {
    init {
        cause?.let(::initCause)
    }
}

/** LLM 장비는 한 번에 한 답만 만든다. 줄이 길어 기다리다 포기했다 — 잠시 뒤 다시 하면 된다. */
class RagBusyException : ConflictException("다른 질문을 처리하는 중입니다. 잠시 뒤 다시 시도해 주세요", ErrorCode.RAG_BUSY)

class EmptyRagTextException : ValidationException("글이 비어 있습니다", ErrorCode.VALIDATION_FAILED)

class InvalidChatRequestException(
    message: String,
) : ValidationException(message, ErrorCode.VALIDATION_FAILED)
