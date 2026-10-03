package com.gijun.main.shared.infrastructure.config

import ai.koog.embeddings.base.Embedder
import ai.koog.prompt.executor.model.PromptExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/** 스위치가 실제로 듣는지 본다. 네트워크는 타지 않는다 — 클라이언트는 만들기만 하고 부르지 않는다. */
class KoogConfigTest {
    private val runner = ApplicationContextRunner().withUserConfiguration(KoogConfig::class.java)

    @Test
    fun `꺼 두면 Koog 빈이 하나도 없다`() {
        runner.run { context ->
            assertFalse(context.containsBean("promptExecutor"))
            assertFalse(context.containsBean("embedder"))
        }
    }

    @Test
    fun `켜면 실행기와 임베더가 생기고 설정값이 들어간다`() {
        runner
            .withPropertyValues("rag.enabled=true", "rag.chat.model=other", "rag.chat.base-url=http://localhost:1")
            .run { context ->
                assertTrue(context.getBeansOfType(PromptExecutor::class.java).isNotEmpty())
                assertTrue(context.getBeansOfType(Embedder::class.java).isNotEmpty())
                val properties = context.getBean(RagProperties::class.java)
                assertEquals("other", properties.chat.model)
                assertEquals("http://localhost:1", properties.chat.baseUrl)
                // 안 적은 값은 기본값이 남는다.
                assertEquals(1_024, properties.embedding.dimensions)
            }
    }
}
