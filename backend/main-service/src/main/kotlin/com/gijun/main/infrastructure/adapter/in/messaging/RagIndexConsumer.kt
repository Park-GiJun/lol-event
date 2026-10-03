package com.gijun.main.infrastructure.adapter.`in`.messaging

import com.gijun.main.application.port.`in`.IndexMatchRagDocumentsUseCase
import com.gijun.main.infrastructure.adapter.out.messaging.MatchEventKafkaAdapter
import com.gijun.main.shared.infrastructure.web.common.matchIdOf
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * 경기가 저장되면 그 경기에 딸린 검색 문서를 다시 쓴다.
 *
 * 같은 신호를 두 번 받아도 해가 없다 — 글이 그대로인 문서는 임베딩을 건너뛴다.
 *
 * 레이팅 계산도 같은 경기의 다른 신호로 따로 돈다. 순서를 맞추지 않으므로 플레이어 문서의 Elo 가
 * 한 경기 늦을 수 있다 — 다음 경기나 전체 색인 때 따라온다. 챗봇은 Elo 를 문서가 아니라 tool 로
 * 읽으므로 답에는 영향이 없다.
 */
@Component
class RagIndexConsumer(
    private val indexMatchRagDocumentsUseCase: IndexMatchRagDocumentsUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [MatchEventKafkaAdapter.TOPIC_RAG_INDEX], groupId = "rag-index-consumer")
    fun onMatchSaved(record: ConsumerRecord<String, String>) {
        try {
            indexMatchRagDocumentsUseCase.indexMatchRagDocuments(matchIdOf(record.key()))
        } catch (e: Exception) {
            // 다시 던지면 같은 메시지를 계속 재시도한다. LLM 장비가 꺼져 있을 때 그러면 로그만 쌓인다.
            // 놓친 문서는 전체 색인이 메운다.
            log.warn("RAG 색인 실패 — matchId={}: {}", record.key(), e.message)
        }
    }
}
