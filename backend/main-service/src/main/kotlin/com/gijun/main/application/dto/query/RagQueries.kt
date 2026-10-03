package com.gijun.main.application.dto.query

import com.gijun.main.domain.rag.enums.RagDocumentType

data class SearchRagDocumentsQuery(
    val text: String,
    val limit: Int = DEFAULT_LIMIT,
    /** 주면 그 종류 안에서만 찾는다. */
    val docType: RagDocumentType? = null,
) {
    companion object {
        const val DEFAULT_LIMIT = 5

        /** 글이 길어 많이 넣으면 모델의 컨텍스트(32k)를 먹는다. */
        const val MAX_LIMIT = 20
    }
}
