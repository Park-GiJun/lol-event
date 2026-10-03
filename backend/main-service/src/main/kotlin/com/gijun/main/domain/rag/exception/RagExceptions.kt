package com.gijun.main.domain.rag.exception

import com.gijun.main.shared.domain.exception.ConflictException
import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.ValidationException

/** `rag.enabled=false` 인데 LLM 이 필요한 일을 시켰다. LLM 장비를 켜고 설정을 바꾸기 전에는 풀리지 않는다. */
class RagDisabledException : ConflictException("RAG 가 꺼져 있습니다", ErrorCode.RAG_DISABLED)

/** LLM 장비는 한 번에 한 답만 만든다. 줄이 길어 기다리다 포기했다 — 잠시 뒤 다시 하면 된다. */
class RagBusyException : ConflictException("다른 질문을 처리하는 중입니다. 잠시 뒤 다시 시도해 주세요", ErrorCode.RAG_BUSY)

class EmptyRagTextException : ValidationException("글이 비어 있습니다", ErrorCode.VALIDATION_FAILED)

class InvalidChatRequestException(
    message: String,
) : ValidationException(message, ErrorCode.VALIDATION_FAILED)
