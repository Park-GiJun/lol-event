package com.gijun.main.infrastructure.adapter.`in`.messaging

import com.gijun.main.application.port.`in`.CalculateEloForMatchUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * lol.elo.calculate 토픽 구독
 * 매치 저장 완료 후 발행된 matchId를 받아 해당 매치의 Elo를 계산한다.
 */
@Component
class EloCalculateConsumer(
    private val calculateEloForMatchUseCase: CalculateEloForMatchUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["lol.elo.calculate"], groupId = "elo-calculate-consumer")
    fun onEloCalculate(record: ConsumerRecord<String, String>) {
        val matchId = record.key()
        try {
            calculateEloForMatchUseCase.calculateForMatch(matchId)
            log.info("Elo 계산 완료: $matchId")
        } catch (e: Exception) {
            log.error("Elo 계산 실패: $matchId — ${e.message}", e)
            throw e
        }
    }
}
