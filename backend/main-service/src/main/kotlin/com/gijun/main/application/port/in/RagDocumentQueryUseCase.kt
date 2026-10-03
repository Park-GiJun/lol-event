package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.SearchRagDocumentsQuery
import com.gijun.main.application.dto.result.RagDocumentResult

interface SearchRagDocumentsUseCase {
    /** 질문과 뜻이 가까운 글을 가까운 순으로 돌려준다. */
    fun searchRagDocuments(query: SearchRagDocumentsQuery): List<RagDocumentResult>
}
