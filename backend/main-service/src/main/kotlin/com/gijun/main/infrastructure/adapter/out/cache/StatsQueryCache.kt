package com.gijun.main.infrastructure.adapter.out.cache

import com.gijun.main.application.port.out.StatsCachePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 통계 핸들러용 인메모리 TTL 캐시.
 * 동일한 mode/파라미터 조합에 대해 5분간 결과를 재사용하여
 * 매 요청마다 DB 풀스캔하는 것을 방지한다.
 *
 * 두 가지를 같이 막는다.
 *
 *  - **만료 항목 누적.** 키에 파라미터가 붙는다(`champion-matchup:$champion:$vsChampion:$mode`).
 *    챔피언 조합만으로도 키가 수만 개까지 벌어지는데, 예전에는 만료된 항목을 evict 호출
 *    전까지 아무도 치우지 않아서 맵이 한 방향으로만 자랐다. 이제 쓰기 때마다 만료분을
 *    훑어 내고, 그래도 상한을 넘으면 만료가 임박한 순으로 버린다.
 *
 *  - **같은 키에 동시 진입.** getOrCompute 가 원자적이지 않아서, 캐시가 빈 키에 요청이
 *    N개 겹치면 N개가 각자 풀스캔을 돌았다. 키 해시로 고른 락 하나를 잡고 다시 확인해서
 *    한 번만 계산하게 한다. 락은 개수가 고정이라 락 자체가 늘어나지는 않는다.
 */
@Component
class StatsQueryCache : StatsCachePort {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class CacheEntry<T>(val value: T, val expiresAt: Long)

    private val store = ConcurrentHashMap<String, CacheEntry<*>>()

    /** 키 해시로 고르는 고정 개수 락. 키마다 락을 만들면 그게 다시 샌다. */
    private val locks = Array(LOCK_STRIPES) { Any() }

    /** 마지막 청소 시각. 매 쓰기마다 전체를 훑지 않으려고 둔다. */
    private val lastSweepAt = AtomicLong(0)

    companion object {
        private const val TTL_MILLIS = 5 * 60 * 1000L // 5분
        private const val LOCK_STRIPES = 64
        private const val MAX_ENTRIES = 10_000
        private const val SWEEP_INTERVAL_MILLIS = 60 * 1000L // 1분
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrCompute(key: String, compute: () -> T): T {
        val now = System.currentTimeMillis()
        (store[key] as? CacheEntry<T>)?.let { if (it.expiresAt > now) return it.value }

        // 같은 키끼리만 줄을 세운다. 다른 키는 다른 스트라이프라 서로 안 막는다.
        synchronized(locks[lockIndex(key)]) {
            val recheckedAt = System.currentTimeMillis()
            (store[key] as? CacheEntry<T>)?.let { if (it.expiresAt > recheckedAt) return it.value }

            val result = compute()
            store[key] = CacheEntry(result, recheckedAt + TTL_MILLIS)
            maintain(recheckedAt)
            return result
        }
    }

    override fun evictAll() {
        store.clear()
        log.info("[StatsQueryCache] 전체 캐시 초기화")
    }

    override fun evictByPrefix(prefix: String) {
        store.keys.removeIf { it.startsWith(prefix) }
    }

    private fun lockIndex(key: String): Int = (key.hashCode() and 0x7fffffff) % LOCK_STRIPES

    /** 만료분 청소와 상한 강제. 쓰기 경로에서만 부른다. */
    private fun maintain(now: Long) {
        val overCapacity = store.size > MAX_ENTRIES
        val dueForSweep = now - lastSweepAt.get() > SWEEP_INTERVAL_MILLIS
        if (!overCapacity && !dueForSweep) return
        // 여러 스레드가 겹쳐 들어와도 한 번만 돌게 한다.
        if (!lastSweepAt.compareAndSet(lastSweepAt.get(), now)) return

        store.entries.removeIf { it.value.expiresAt <= now }

        if (store.size > MAX_ENTRIES) {
            // 아직 살아 있는 항목뿐이므로 만료가 가까운 순으로 버린다.
            val excess = store.size - MAX_ENTRIES
            store.entries
                .sortedBy { it.value.expiresAt }
                .take(excess)
                .forEach { store.remove(it.key, it.value) }
            log.info("[StatsQueryCache] 상한 초과로 ${excess}개 정리 — 현재 ${store.size}개")
        }
    }
}
