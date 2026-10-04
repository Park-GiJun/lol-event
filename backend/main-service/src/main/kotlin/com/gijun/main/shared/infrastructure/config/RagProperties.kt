package com.gijun.main.shared.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * RAG(질의응답) 설정. 값은 `application.yaml` 의 `rag.*` 에 있다.
 *
 * LLM 은 집 안 장비(BC-250)의 llama.cpp 서버 둘이다 — 둘 다 OpenAI 호환 API 를 낸다.
 * 스위치는 없다. 그 장비가 꺼져 있으면 AI 기능만 `RAG_UNAVAILABLE` 로 실패하고 나머지는 그대로 돈다
 * — 클라이언트는 부를 때 연결하므로 기동에는 영향이 없다.
 */
@ConfigurationProperties(prefix = "rag")
data class RagProperties(
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
        /**
         * 낮게 둔다. 주지 않으면 llama-server 기본값으로 돌아 같은 질문에 tool 을 부를 때와 안 부를 때가 갈린다 —
         * 이어지는 질문에서 tool 없이 숫자를 지어내는 답이 네 번에 한 번꼴로 나왔다.
         */
        val temperature: Double = 0.2,
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
