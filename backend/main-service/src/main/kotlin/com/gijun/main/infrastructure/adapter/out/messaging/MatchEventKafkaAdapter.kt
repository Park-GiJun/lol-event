package com.gijun.main.infrastructure.adapter.out.messaging

import com.gijun.main.application.port.out.messaging.MatchEventPublishPort
import com.gijun.main.shared.domain.vo.MatchId
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class MatchEventKafkaAdapter(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : MatchEventPublishPort {
    override fun publishRatingRequested(matchId: MatchId) {
        kafkaTemplate.send(TOPIC_ELO_CALCULATE, matchId.value, matchId.value)
    }

    override fun publishStatsRebuildRequested(matchId: MatchId) {
        kafkaTemplate.send(TOPIC_STATS_REBUILD, matchId.value, PAYLOAD_MATCH_SAVED)
    }

    override fun publishRagIndexRequested(matchId: MatchId) {
        kafkaTemplate.send(TOPIC_RAG_INDEX, matchId.value, PAYLOAD_MATCH_SAVED)
    }

    companion object {
        const val TOPIC_ELO_CALCULATE = "lol.elo.calculate"
        const val TOPIC_STATS_REBUILD = "lol.stats.rebuild"
        const val TOPIC_RAG_INDEX = "lol.rag.index"
        private const val PAYLOAD_MATCH_SAVED = "match_saved"
    }
}
