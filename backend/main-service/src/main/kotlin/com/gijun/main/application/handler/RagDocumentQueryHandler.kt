package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.SearchRagDocumentsQuery
import com.gijun.main.application.dto.result.RagDocumentResult
import com.gijun.main.application.port.`in`.SearchRagDocumentsUseCase
import com.gijun.main.application.port.out.external.TextEmbeddingPort
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import com.gijun.main.domain.rag.exception.EmptyRagTextException
import org.springframework.stereotype.Service

/** 질문과 뜻이 가까운 글을 찾는다. 에이전트의 검색 tool 이 이걸 부른다. */
@Service
class RagDocumentQueryHandler(
    private val textEmbeddingPort: TextEmbeddingPort,
    private val ragDocumentQueryPersistencePort: RagDocumentQueryPersistencePort,
) : SearchRagDocumentsUseCase {
    override fun searchRagDocuments(query: SearchRagDocumentsQuery): List<RagDocumentResult> {
        val text = query.text.trim()
        if (text.isEmpty()) throw EmptyRagTextException()

        // 모델이 tool 인자로 엉뚱한 수를 넣을 수 있다. 거절하지 않고 범위 안으로 당긴다.
        val limit = query.limit.coerceIn(1, SearchRagDocumentsQuery.MAX_LIMIT)

        return ragDocumentQueryPersistencePort
            .findNearest(textEmbeddingPort.embed(text), limit, query.docType)
            .map(RagDocumentResult::from)
    }
}
