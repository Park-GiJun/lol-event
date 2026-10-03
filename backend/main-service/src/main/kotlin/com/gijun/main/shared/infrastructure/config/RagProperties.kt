package com.gijun.main.shared.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * RAG(질의응답) 설정. 값은 `application.yaml` 의 `rag.*` 에 있다.
 *
 * LLM 은 집 안 장비(BC-250)의 llama.cpp 서버 둘이다 — 둘 다 OpenAI 호환 API 를 낸다.
 * 그 장비가 꺼져 있어도 통계 사이트는 떠야 하므로 기본은 꺼 둔다([enabled]).
 */
@ConfigurationProperties(prefix = "rag")
data class RagProperties(
    /** false 면 Koog 빈을 하나도 만들지 않는다. */
    val enabled: Boolean = false,
    val chat: Chat = Chat(),
    val embedding: Embedding = Embedding(),
) {
    data class Chat(
        /** `/v1` 을 붙이지 않는다 — 경로는 Koog 가 붙인다. */
        val baseUrl: String = "http://172.30.1.54:8080",
        /** llama-server 의 `--alias`. */
        val model: String = "qwen",
        /** llama-server 에 `--api-key` 를 걸기 전까지는 아무 값이나 통한다. */
        val apiKey: String = "none",
        val contextLength: Long = 32_768,
        val maxOutputTokens: Long = 2_048,
        /** 답변 하나가 수십 초 걸린다. tool 을 여러 번 부르면 더 길어진다. */
        val requestTimeoutMillis: Long = 120_000,
    )

    data class Embedding(
        val baseUrl: String = "http://172.30.1.54:8081",
        val model: String = "pqnet/bge-m3-gguf:F16",
        val apiKey: String = "none",
        /** bge-m3 의 차원. `V21` 마이그레이션의 `vector(1024)` 와 같아야 한다. */
        val dimensions: Int = 1_024,
        val requestTimeoutMillis: Long = 30_000,
    )
}
