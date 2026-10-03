package com.gijun.main.infrastructure.adapter.out.external

import ai.koog.embeddings.base.Embedder
import com.gijun.main.application.port.out.external.TextEmbeddingPort
import com.gijun.main.shared.infrastructure.config.RagProperties
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

/** 임베딩 서버(bge-m3)에 Koog 로 물어본다. */
@Component
class KoogTextEmbeddingAdapter(
    private val embedder: Embedder,
    private val properties: RagProperties,
) : TextEmbeddingPort {
    override fun embed(text: String): List<Float> {
        val values = runBlocking { embedder.embed(text) }.values

        // 모델을 바꾸면 차원이 달라진다. 그대로 넣으면 DB 가 알아듣기 어려운 오류를 내므로 여기서 먼저 말한다.
        check(values.size == properties.embedding.dimensions) {
            "임베딩 차원이 설정과 다르다: 받은 것 ${values.size}, 설정 ${properties.embedding.dimensions} " +
                "— 모델을 바꿨으면 rag.embedding.dimensions 와 rag_documents.embedding 의 차원을 같이 바꾼다."
        }
        return values.map { it.toFloat() }
    }
}
