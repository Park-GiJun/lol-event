package com.gijun.main.application.dto.query

import com.gijun.main.domain.rag.enums.RagDocumentType

/**
 * @param position TOP / JUNGLE / MID / ADC / SUPPORT
 * @param allies 아군 챔피언의 영문 키.
 */
data class GetAllyPicksQuery(
    val position: String,
    val allies: List<String>,
)

/** 사람이 쓴 그대로 — 고를 라인의 이름과 아군 챔피언 이름들. */
data class DescribePickQuery(
    val position: String,
    val allies: List<String>,
)

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
