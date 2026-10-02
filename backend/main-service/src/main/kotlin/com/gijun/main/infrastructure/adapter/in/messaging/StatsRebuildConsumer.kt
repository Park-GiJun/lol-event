package com.gijun.main.infrastructure.adapter.`in`.messaging

import com.gijun.main.application.port.`in`.TriggerStatsAggregationUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * lol.stats.rebuild 토픽 구독 — 새 매치 저장 후 발행된 재집계 신호를 받아 배치 실행
 * 5분 내 중복 실행 방지(스로틀) 처리
 */
@Component
class StatsRebuildConsumer(
    private val triggerStatsAggregationUseCase: TriggerStatsAggregationUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val lastLaunchedAt = AtomicLong(0L)

    @KafkaListener(topics = ["lol.stats.rebuild"], groupId = "stats-rebuild-consumer")
    fun onRebuildSignal(record: ConsumerRecord<String, String>) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastLaunchedAt.get()

        if (elapsed < THROTTLE_MS) {
            log.debug("통계 재집계 스로틀 — 마지막 실행 후 ${elapsed / 1000}초 경과 (최소 ${THROTTLE_MS / 1000}초)")
            return
        }

        lastLaunchedAt.set(now)
        log.info("통계 재집계 트리거 — matchId=${record.key()}")
        triggerStatsAggregationUseCase.triggerStatsAggregation(REASON)
    }

    private companion object {
        /** 5분. 매치가 연달아 들어와도 재집계는 이 간격으로 한 번만 돈다. */
        private const val THROTTLE_MS = 5 * 60 * 1000L
        private const val REASON = "kafka-trigger"
    }
}
