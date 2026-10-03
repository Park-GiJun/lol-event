package com.gijun.main.shared.infrastructure.config

import ai.koog.embeddings.base.Embedder
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Koog 가 LLM 서버 둘에 붙는 자리.
 *
 * 채팅과 임베딩은 **다른 서버**(포트)라 클라이언트도 둘이다. 한 클라이언트로 합치면 임베딩 요청이
 * 채팅 서버로 간다.
 *
 * `rag.enabled=false` 면 이 클래스의 빈이 하나도 없다. 이 빈을 받는 쪽도 같은 조건을 달아야
 * 꺼 둔 상태에서 기동이 된다.
 */
@Configuration
@EnableConfigurationProperties(RagProperties::class)
@ConditionalOnProperty(prefix = "rag", name = ["enabled"], havingValue = "true")
class KoogConfig(
    private val properties: RagProperties,
) {
    /**
     * 채팅 모델. llama.cpp 의 모델은 Koog 의 `OpenAIModels` 목록에 없으므로 직접 적는다.
     *
     * capability 는 Koog 가 요청을 보내기 전에 검사하는 값이다 — tool 을 쓰려면 [LLMCapability.Tools]
     * 가 있어야 한다. 서버가 실제로 못 하는 것을 적으면 그때는 서버가 400 을 낸다.
     */
    @Bean
    @Qualifier(CHAT)
    fun chatModel(): LLModel =
        LLModel(
            provider = LLMProvider.OpenAI,
            id = properties.chat.model,
            capabilities =
                listOf(
                    LLMCapability.Completion,
                    LLMCapability.Temperature,
                    LLMCapability.Tools,
                    LLMCapability.ToolChoice,
                    LLMCapability.Schema.JSON.Basic,
                    LLMCapability.OpenAIEndpoint.Completions,
                ),
            contextLength = properties.chat.contextLength,
            maxOutputTokens = properties.chat.maxOutputTokens,
        )

    @Bean
    @Qualifier(EMBEDDING)
    fun embeddingModel(): LLModel =
        LLModel(
            provider = LLMProvider.OpenAI,
            id = properties.embedding.model,
            capabilities = listOf(LLMCapability.Embed),
            contextLength = EMBEDDING_CONTEXT_LENGTH,
        )

    @Bean(destroyMethod = "close")
    @Qualifier(CHAT)
    fun chatClient(): OpenAILLMClient =
        OpenAILLMClient(
            apiKey = properties.chat.apiKey,
            settings =
                OpenAIClientSettings(
                    baseUrl = properties.chat.baseUrl,
                    timeoutConfig = ConnectionTimeoutConfig(requestTimeoutMillis = properties.chat.requestTimeoutMillis),
                ),
        )

    @Bean(destroyMethod = "close")
    @Qualifier(EMBEDDING)
    fun embeddingClient(): OpenAILLMClient =
        OpenAILLMClient(
            apiKey = properties.embedding.apiKey,
            settings =
                OpenAIClientSettings(
                    baseUrl = properties.embedding.baseUrl,
                    timeoutConfig = ConnectionTimeoutConfig(requestTimeoutMillis = properties.embedding.requestTimeoutMillis),
                ),
        )

    /** 에이전트(`AIAgent`)에 넘기는 실행기. 채팅 서버 하나만 안다. */
    @Bean
    fun promptExecutor(
        @Qualifier(CHAT) chatClient: OpenAILLMClient,
    ): PromptExecutor = MultiLLMPromptExecutor(chatClient)

    /** 글 → 1024 차원 벡터. */
    @Bean
    fun embedder(
        @Qualifier(EMBEDDING) embeddingClient: OpenAILLMClient,
        @Qualifier(EMBEDDING) embeddingModel: LLModel,
    ): Embedder = LLMEmbedder(embeddingClient, embeddingModel)

    companion object {
        const val CHAT = "ragChat"
        const val EMBEDDING = "ragEmbedding"

        /** bge-m3 가 한 번에 읽는 토큰 수. */
        private const val EMBEDDING_CONTEXT_LENGTH = 8_192L
    }
}
