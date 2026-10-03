package com.gijun.main.infrastructure.adapter.out.external

import ai.koog.embeddings.base.Embedder
import ai.koog.embeddings.base.Vector
import com.gijun.main.shared.infrastructure.config.RagProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TextEmbeddingAdaptersTest {
    @Test
    fun `설정한 차원의 벡터를 그대로 돌려준다`() {
        val adapter = KoogTextEmbeddingAdapter(fixedEmbedder(listOf(0.5, 0.25)), properties(dimensions = 2))

        assertEquals(listOf(0.5f, 0.25f), adapter.embed("x"))
    }

    @Test
    fun `차원이 설정과 다르면 DB 에 가기 전에 멈춘다`() {
        // 임베딩 모델만 바꾸고 테이블은 그대로 둔 경우다.
        val adapter = KoogTextEmbeddingAdapter(fixedEmbedder(listOf(0.5, 0.25, 0.1)), properties(dimensions = 2))

        assertThrows(IllegalStateException::class.java) { adapter.embed("x") }
    }

    private fun properties(dimensions: Int) = RagProperties(embedding = RagProperties.Embedding(dimensions = dimensions))

    private fun fixedEmbedder(values: List<Double>) =
        object : Embedder {
            override suspend fun embed(text: String): Vector = Vector(values)

            override fun diff(
                embedding1: Vector,
                embedding2: Vector,
            ): Double = 0.0
        }
}
