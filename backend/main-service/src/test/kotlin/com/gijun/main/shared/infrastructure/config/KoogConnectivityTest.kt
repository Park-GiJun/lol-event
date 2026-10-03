package com.gijun.main.shared.infrastructure.config

import ai.koog.prompt.dsl.prompt
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * LLM 서버 둘에 실제로 닿는지 본다. **집 안 망에서만 돈다** — 그래서 평소 빌드에서는 건너뛴다.
 *
 * ```
 * RAG_SMOKE=1 ./gradlew :main-service:test --tests '*KoogConnectivityTest'
 * ```
 *
 * 서버 주소나 모델을 바꿨을 때, Koog 버전을 올렸을 때 돌려 본다.
 */
@EnabledIfEnvironmentVariable(named = "RAG_SMOKE", matches = "1")
class KoogConnectivityTest {
    private val properties = RagProperties(enabled = true)
    private val config = KoogConfig(properties)

    @Test
    fun `채팅 서버가 답한다`() =
        runBlocking {
            val client = config.chatClient()
            try {
                val response =
                    config.promptExecutor(client).execute(
                        prompt = prompt("smoke") { user("한 단어로만 답해. 한국의 수도는?") },
                        model = config.chatModel(),
                        tools = emptyList(),
                    )
                val answer = response.textContent()
                println("chat → $answer")
                assertTrue(answer.contains("서울")) { "뜻밖의 답: $answer" }
            } finally {
                client.close()
            }
        }

    @Test
    fun `임베딩 서버가 설정한 차원의 벡터를 준다`() =
        runBlocking {
            val client = config.embeddingClient()
            try {
                val vector = config.embedder(client, config.embeddingModel()).embed("라인전이 강한 탑 라이너")
                println("embedding → ${vector.values.size} 차원")
                assertEquals(properties.embedding.dimensions, vector.values.size)
            } finally {
                client.close()
            }
        }
}
