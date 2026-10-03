package com.gijun.main.shared.infrastructure.config

import ai.koog.embeddings.base.Embedder
import ai.koog.prompt.executor.model.PromptExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/** 네트워크는 타지 않는다 — 클라이언트는 만들기만 하고 부르지 않는다. */
class KoogConfigTest {
    private val runner = ApplicationContextRunner().withUserConfiguration(KoogConfig::class.java)

    @Test
    fun `LLM 장비가 없어도 빈은 만들어진다`() {
        // 닿지 않는 주소다. 클라이언트는 부를 때 연결하므로, 장비가 꺼져 있어도 서비스 기동은 막히지 않는다.
        runner
            .withPropertyValues("rag.chat.base-url=http://localhost:1", "rag.embedding.base-url=http://localhost:1")
            .run { context ->
                assertTrue(context.getBeansOfType(PromptExecutor::class.java).isNotEmpty())
                assertTrue(context.getBeansOfType(Embedder::class.java).isNotEmpty())
            }
    }

    @Test
    fun `설정값이 들어가고 안 적은 값은 기본값이 남는다`() {
        runner
            .withPropertyValues("rag.chat.model=other", "rag.chat.base-url=http://localhost:1")
            .run { context ->
                val properties = context.getBean(RagProperties::class.java)
                assertEquals("other", properties.chat.model)
                assertEquals("http://localhost:1", properties.chat.baseUrl)
                assertEquals(1_024, properties.embedding.dimensions)
            }
    }
}
