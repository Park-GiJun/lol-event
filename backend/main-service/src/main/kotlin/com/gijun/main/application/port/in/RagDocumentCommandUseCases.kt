package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.command.DeleteRagDocumentCommand
import com.gijun.main.application.dto.command.IndexRagDocumentCommand
import com.gijun.main.application.dto.result.IndexRagDocumentResult

interface IndexRagDocumentUseCase {
    /** 글을 임베딩해 저장한다. 같은 신원의 글이 그대로면 임베딩을 건너뛴다. */
    fun indexRagDocument(command: IndexRagDocumentCommand): IndexRagDocumentResult
}

interface DeleteRagDocumentUseCase {
    fun deleteRagDocument(command: DeleteRagDocumentCommand)
}
