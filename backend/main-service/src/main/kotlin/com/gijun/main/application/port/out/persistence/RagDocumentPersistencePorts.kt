package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.rag.enums.RagDocumentType
import com.gijun.main.domain.rag.model.RagDocumentModel
import com.gijun.main.domain.rag.model.RagSearchHitModel

interface RagDocumentCommandPersistencePort {
    /** `(docType, sourceKey)` 가 이미 있으면 글과 벡터를 덮어쓴다. */
    fun upsert(
        document: RagDocumentModel,
        embedding: List<Float>,
    )

    /** @return 지운 건수(0 또는 1) */
    fun delete(
        docType: RagDocumentType,
        sourceKey: String,
    ): Int
}

interface RagDocumentQueryPersistencePort {
    /**
     * [embedding] 에 가까운 순으로 [limit] 건.
     *
     * @param docType 주면 그 종류 안에서만 찾는다.
     */
    fun findNearest(
        embedding: List<Float>,
        limit: Int,
        docType: RagDocumentType?,
    ): List<RagSearchHitModel>

    /** 저장된 글. 내용이 그대로면 다시 임베딩하지 않으려고 본다. */
    fun findContent(
        docType: RagDocumentType,
        sourceKey: String,
    ): String?

    /** 그 종류로 저장된 문서의 키 전부. 원본이 사라진 문서를 골라내는 데 쓴다. */
    fun findSourceKeys(docType: RagDocumentType): List<String>

    fun countByType(): Map<RagDocumentType, Int>
}
