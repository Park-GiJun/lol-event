package com.gijun.main.domain.rag.model

import com.gijun.main.domain.rag.enums.RagDocumentType
import java.time.Instant

/**
 * 검색 대상 글 한 건. `(docType, sourceKey)` 가 신원이다 — 같은 신원으로 다시 넣으면 덮어쓴다.
 *
 * 글은 **코드로 집계한 사실을 문장으로 옮긴 것**이어야 한다. 숫자를 LLM 이 지어내지 않게 하려는
 * 것이므로, 이 글을 LLM 으로 만들면 그 원칙이 여기서 무너진다.
 */
data class RagDocumentModel(
    val docType: RagDocumentType,
    val sourceKey: String,
    val content: String,
)

/** 검색에 걸린 문서. */
data class RagSearchHitModel(
    val document: RagDocumentModel,
    /** 코사인 거리. 0 이면 같은 방향, 클수록 멀다(최대 2). */
    val distance: Double,
    val updatedAt: Instant,
)
