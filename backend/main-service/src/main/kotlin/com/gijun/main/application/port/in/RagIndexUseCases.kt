package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.RagIndexStatusResult
import com.gijun.main.application.dto.result.RagIndexSummaryResult
import com.gijun.main.application.dto.result.StartRagReindexResult
import com.gijun.main.shared.domain.vo.MatchId

interface IndexMatchRagDocumentsUseCase {
    /**
     * 경기 한 판이 들어왔을 때. 그 경기의 리뷰와, 거기 나온 열 명·열 챔피언의 문서를 다시 쓴다.
     * 글이 그대로인 문서는 임베딩하지 않는다.
     */
    fun indexMatchRagDocuments(matchId: MatchId): RagIndexSummaryResult
}

interface StartRagReindexUseCase {
    /** 전체를 처음부터 다시 쓴다. 뒤에서 돌고 바로 돌아온다 — 진행은 [GetRagIndexStatusUseCase] 로 본다. */
    fun startRagReindex(): StartRagReindexResult
}

interface GetRagIndexStatusUseCase {
    fun getRagIndexStatus(): RagIndexStatusResult
}
