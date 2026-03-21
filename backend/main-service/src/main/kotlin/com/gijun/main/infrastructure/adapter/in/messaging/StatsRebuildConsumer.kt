package com.gijun.main.infrastructure.adapter.`in`.messaging

import com.gijun.main.infrastructure.adapter.`in`.scheduler.StatsAggregationScheduler
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
    private val scheduler: StatsAggregationScheduler,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val lastLaunchedAt = AtomicLong(0L)
    private val THROTTLE_MS = 5 * 60 * 1000L  // 5분

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
        scheduler.launchJob(reason = "kafka-trigger")
    }
}
