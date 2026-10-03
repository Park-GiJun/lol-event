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

/** 한 번의 색인이 한 일. */
data class RagIndexSummaryResult(
    /** 다시 쓴 문서 수. */
    val total: Int,
    /** 그중 글이 바뀌어 실제로 임베딩한 수. */
    val embedded: Int,
    val failed: Int,
)

data class StartRagReindexResult(
    /** false 면 이미 돌고 있어서 새로 시작하지 않았다. */
    val started: Boolean,
)

data class RagIndexStatusResult(
    val running: Boolean,
    /** 이번(또는 마지막) 전체 색인이 쓸 문서 수. */
    val total: Int,
    val processed: Int,
    val embedded: Int,
    val failed: Int,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    /** 마지막으로 실패한 문서와 이유. */
    val lastError: String?,
    /** 지금 저장돼 있는 문서 수. 키는 [RagDocumentType] 이름. */
    val documentCounts: Map<String, Int>,
)

data class ChatAnswerResult(
    val reply: String,
)

data class ChampionSynergyResult(
    val champion: String,
    /** 이 챔피언이 나온 판수. */
    val games: Int,
    /** 같이 한 판이 많은 순. */
    val allies: List<AllyChampionStat>,
)

data class AllyChampionStat(
    val champion: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
)
