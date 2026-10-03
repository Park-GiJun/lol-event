package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.DeleteRagDocumentCommand
import com.gijun.main.application.dto.command.IndexRagDocumentCommand
import com.gijun.main.application.dto.result.IndexRagDocumentResult
import com.gijun.main.application.port.`in`.DeleteRagDocumentUseCase
import com.gijun.main.application.port.`in`.IndexRagDocumentUseCase
import com.gijun.main.application.port.out.external.TextEmbeddingPort
import com.gijun.main.application.port.out.persistence.RagDocumentCommandPersistencePort
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import com.gijun.main.domain.rag.exception.EmptyRagTextException
import com.gijun.main.domain.rag.model.RagDocumentModel
import org.springframework.stereotype.Service

/**
 * 검색 대상 글을 넣고 뺀다.
 *
 * **`@Transactional` 을 걸지 않는다.** 임베딩은 LLM 장비까지 다녀오는 HTTP 호출이라, 트랜잭션 안에서
 * 하면 그동안 DB 커넥션을 쥐고 있다. 쓰기는 upsert 한 문장이라 묶을 것도 없다.
 */
@Service
class RagDocumentCommandHandler(
    private val textEmbeddingPort: TextEmbeddingPort,
    private val ragDocumentCommandPersistencePort: RagDocumentCommandPersistencePort,
    private val ragDocumentQueryPersistencePort: RagDocumentQueryPersistencePort,
) : IndexRagDocumentUseCase,
    DeleteRagDocumentUseCase {
    override fun indexRagDocument(command: IndexRagDocumentCommand): IndexRagDocumentResult {
        val content = command.content.trim()
        if (content.isEmpty()) throw EmptyRagTextException()

        // 배치는 매번 전원의 글을 다시 만든다. 대부분은 그대로라서 임베딩을 다시 할 이유가 없다.
        val stored = ragDocumentQueryPersistencePort.findContent(command.docType, command.sourceKey)
        if (stored == content) return IndexRagDocumentResult(embedded = false)

        ragDocumentCommandPersistencePort.upsert(
            document = RagDocumentModel(command.docType, command.sourceKey, content),
            embedding = textEmbeddingPort.embed(content),
        )
        return IndexRagDocumentResult(embedded = true)
    }

    override fun deleteRagDocument(command: DeleteRagDocumentCommand) {
        ragDocumentCommandPersistencePort.delete(command.docType, command.sourceKey)
    }
}
