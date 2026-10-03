package com.gijun.main.application.dto.command

import com.gijun.main.domain.rag.enums.RagDocumentType

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
