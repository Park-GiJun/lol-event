package com.gijun.main.infrastructure.adapter.`in`.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.application.dto.command.IngestMatchCommand
import com.gijun.main.application.dto.command.MatchInput
import com.gijun.main.application.port.`in`.IngestMatchUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/** `lol.match.events` 구독 — 이벤트로 들어온 경기 한 판을 저장한다. */
@Component
class MatchEventConsumer(
    private val objectMapper: ObjectMapper,
    private val ingestMatchUseCase: IngestMatchUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["lol.match.events"], groupId = "main-service-match-consumer")
    fun consume(record: ConsumerRecord<String, String>) {
        val matchId = record.key()
        try {
            val input = objectMapper.readValue(record.value(), MatchInput::class.java)
            ingestMatchUseCase.ingestMatch(IngestMatchCommand(input))
        } catch (e: Exception) {
            log.error("매치 처리 실패: $matchId — ${e.message}", e)
            // 다시 던져야 Kafka 가 재시도한다.
            throw e
        }
    }
}
