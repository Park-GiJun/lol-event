package com.gijun.main.application.dto.result

import com.gijun.main.domain.rag.enums.RagDocumentType
import com.gijun.main.domain.rag.model.RagSearchHitModel
import java.time.Instant

data class IndexRagDocumentResult(
    /** false 면 저장된 글과 같아서 임베딩을 건너뛰었다. */
    val embedded: Boolean,
)

data class RagDocumentResult(
    val docType: RagDocumentType,
    val sourceKey: String,
    val content: String,
    /** 코사인 거리. 작을수록 가깝다. */
    val distance: Double,
    val updatedAt: Instant,
) {
    companion object {
        fun from(hit: RagSearchHitModel): RagDocumentResult =
            RagDocumentResult(
                docType = hit.document.docType,
                sourceKey = hit.document.sourceKey,
                content = hit.document.content,
                distance = hit.distance,
                updatedAt = hit.updatedAt,
            )
    }
}
