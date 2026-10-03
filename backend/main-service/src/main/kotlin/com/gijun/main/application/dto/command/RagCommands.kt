package com.gijun.main.application.dto.command

import com.gijun.main.domain.rag.enums.RagDocumentType
import com.gijun.main.domain.rag.model.ChatMessageModel

data class IndexRagDocumentCommand(
    val docType: RagDocumentType,
    /** 종류 안에서 문서를 가리키는 값. 종류별 뜻은 [RagDocumentType] 에 있다. */
    val sourceKey: String,
    val content: String,
)

data class DeleteRagDocumentCommand(
    val docType: RagDocumentType,
    val sourceKey: String,
)

data class AskChatCommand(
    /** 대화 전체. 오래된 것부터, 마지막은 사용자의 질문이어야 한다. */
    val messages: List<ChatMessageModel>,
)
