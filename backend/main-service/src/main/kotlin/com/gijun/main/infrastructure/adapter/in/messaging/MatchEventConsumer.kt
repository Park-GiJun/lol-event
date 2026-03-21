package com.gijun.main.infrastructure.adapter.`in`.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.application.dto.match.command.MatchInput
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.MemberPersistencePort
import com.gijun.main.domain.model.member.Member
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MatchEventConsumer(
    private val objectMapper: ObjectMapper,
    private val matchPersistencePort: MatchPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["lol.match.events"], groupId = "main-service-match-consumer")
    @Transactional
    fun consume(record: ConsumerRecord<String, String>) {
        val matchId = record.key()
        try {
            val input = objectMapper.readValue(record.value(), MatchInput::class.java)

            // 매치 upsert (이미 MatchPersistenceAdapter.save가 upsert 처리)
            matchPersistencePort.save(input.toDomain())
            kafkaTemplate.send("lol.stats.rebuild", matchId, "match_saved")
            kafkaTemplate.send("lol.elo.calculate", matchId, matchId)

            // 참가자 자동 멤버 등록
            autoRegisterMembers(input)

            log.info("매치 처리 완료: $matchId (참가자 ${input.participants.size}명)")
        } catch (e: Exception) {
            log.error("매치 처리 실패: $matchId — ${e.message}", e)
            // 재처리가 필요한 경우 예외를 다시 던지면 Kafka가 재시도
            throw e
        }
    }

    private fun autoRegisterMembers(input: MatchInput) {
        for (p in input.participants) {
            val puuid = p.puuid ?: continue   // puuid 없으면 스킵
            if (!memberPersistencePort.existsByPuuid(puuid)) {
                memberPersistencePort.save(Member(riotId = p.riotId, puuid = puuid))
                log.info("멤버 자동 등록: ${p.riotId} ($puuid)")
            }
        }
    }
}
