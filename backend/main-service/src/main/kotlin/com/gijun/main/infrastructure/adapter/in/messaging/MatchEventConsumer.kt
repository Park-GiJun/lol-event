package com.gijun.main.infrastructure.adapter.`in`.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.application.dto.command.MatchInput
import com.gijun.main.application.port.out.persistence.MatchCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MemberCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.domain.match.service.PositionDetector
import com.gijun.main.domain.member.model.MemberModel
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MatchEventConsumer(
    private val objectMapper: ObjectMapper,
    private val matchCommandPersistencePort: MatchCommandPersistencePort,
    private val memberQueryPersistencePort: MemberQueryPersistencePort,
    private val memberCommandPersistencePort: MemberCommandPersistencePort,
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
            // SaveMatchCommand.toModel() 은 assignedPosition 을 빈 값으로 둔다. 여기서 채우지 않으면
            // 수집기로 들어온 매치는 관리자가 백필을 돌리기 전까지 포지션이 없는 채로 남는다.
            val match = input.toModel()
            match.participants.let { ps ->
                val positioned = PositionDetector.assignPositionsToAll(ps)
                ps.clear()
                ps.addAll(positioned)
            }
            matchCommandPersistencePort.save(match)
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
        val riotIdByPuuid = input.participants.mapNotNull { p -> p.puuid?.let { it to p.riotId } }
        if (riotIdByPuuid.isEmpty()) return

        val existingPuuids = memberQueryPersistencePort.findAllPuuidsByPuuidIn(riotIdByPuuid.map { it.first }).toSet()
        val newcomers = riotIdByPuuid.filter { (puuid, _) -> puuid !in existingPuuids }

        if (newcomers.isNotEmpty()) {
            memberCommandPersistencePort.saveAll(newcomers.map { (puuid, riotId) -> MemberModel(riotId = riotId, puuid = puuid) })
            newcomers.forEach { (puuid, riotId) -> log.info("멤버 자동 등록: $riotId ($puuid)") }
        }
    }
}
