package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.RagIndexStatusResult
import com.gijun.main.application.port.`in`.GetRagIndexStatusUseCase
import com.gijun.main.application.port.out.external.TextEmbeddingPort
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import org.springframework.stereotype.Service

@Service
class RagIndexQueryHandler(
    private val ragDocumentQueryPersistencePort: RagDocumentQueryPersistencePort,
    private val textEmbeddingPort: TextEmbeddingPort,
    private val progress: RagIndexProgress,
) : GetRagIndexStatusUseCase {
    override fun getRagIndexStatus(): RagIndexStatusResult {
        val snapshot = progress.snapshot()
        return RagIndexStatusResult(
            enabled = textEmbeddingPort.isEnabled(),
            running = snapshot.running,
            total = snapshot.total,
            processed = snapshot.processed,
            embedded = snapshot.embedded,
            failed = snapshot.failed,
            startedAt = snapshot.startedAt,
            finishedAt = snapshot.finishedAt,
            lastError = snapshot.lastError,
            documentCounts = ragDocumentQueryPersistencePort.countByType().mapKeys { it.key.name },
        )
    }
}
