package com.gijun.main.infrastructure.adapter.out.cache

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 통계 핸들러용 인메모리 TTL 캐시.
 * 동일한 mode/파라미터 조합에 대해 5분간 결과를 재사용하여
 * 매 요청마다 DB 풀스캔하는 것을 방지한다.
 */
@Component
class StatsQueryCache {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class CacheEntry<T>(val value: T, val expiresAt: Long)

    private val store = ConcurrentHashMap<String, CacheEntry<*>>()

    companion object {
        private const val TTL_MILLIS = 5 * 60 * 1000L // 5분
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrCompute(key: String, compute: () -> T): T {
        val now = System.currentTimeMillis()
        val cached = store[key] as? CacheEntry<T>
        if (cached != null && cached.expiresAt > now) {
            return cached.value
        }
        val result = compute()
        store[key] = CacheEntry(result, now + TTL_MILLIS)
        return result
    }

    fun evictAll() {
        store.clear()
        log.info("[StatsQueryCache] 전체 캐시 초기화")
    }

    fun evictByPrefix(prefix: String) {
        store.keys.removeIf { it.startsWith(prefix) }
    }
}
