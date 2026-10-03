package com.gijun.main.infrastructure.adapter.`in`.web.rag.dto

import com.gijun.main.application.dto.result.ChatAnswerResult
import com.gijun.main.application.dto.result.RagIndexStatusResult
import com.gijun.main.application.dto.result.StartRagReindexResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "챗봇의 답")
data class ChatAnswerResponse(
    val reply: String,
) {
    companion object {
        fun from(result: ChatAnswerResult) = ChatAnswerResponse(reply = result.reply)
    }
}

@Schema(description = "전체 색인 시작 결과")
data class StartRagReindexResponse(
    @field:Schema(description = "false 면 이미 돌고 있어서 새로 시작하지 않았다")
    val started: Boolean,
) {
    companion object {
        fun from(result: StartRagReindexResult) = StartRagReindexResponse(started = result.started)
    }
}

@Schema(description = "색인 상태")
data class RagIndexStatusResponse(
    val running: Boolean,
    @field:Schema(description = "이번(또는 마지막) 전체 색인이 쓸 문서 수")
    val total: Int,
    val processed: Int,
    @field:Schema(description = "글이 바뀌어 실제로 임베딩한 수")
    val embedded: Int,
    val failed: Int,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val lastError: String?,
    @field:Schema(description = "지금 저장된 문서 수. 키는 문서 종류")
    val documentCounts: Map<String, Int>,
) {
    companion object {
        fun from(result: RagIndexStatusResult) =
            RagIndexStatusResponse(
                running = result.running,
                total = result.total,
                processed = result.processed,
                embedded = result.embedded,
                failed = result.failed,
                startedAt = result.startedAt,
                finishedAt = result.finishedAt,
                lastError = result.lastError,
                documentCounts = result.documentCounts,
            )
    }
}
