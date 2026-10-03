package com.gijun.main.domain.rag.exception

import com.gijun.main.shared.domain.exception.ConflictException
import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.ValidationException

/** `rag.enabled=false` 인데 임베딩이 필요한 일을 시켰다. LLM 장비를 켜고 설정을 바꾸기 전에는 풀리지 않는다. */
class RagDisabledException : ConflictException("RAG 가 꺼져 있습니다", ErrorCode.RAG_DISABLED)

class EmptyRagTextException : ValidationException("임베딩할 글이 비어 있습니다", ErrorCode.VALIDATION_FAILED)
