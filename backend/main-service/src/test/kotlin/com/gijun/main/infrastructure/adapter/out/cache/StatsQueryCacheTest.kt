package com.gijun.main.infrastructure.adapter.out.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class StatsQueryCacheTest {

    @Test
    fun `같은 키는 한 번만 계산하고 그 뒤로는 캐시에서 준다`() {
        val cache = StatsQueryCache()
        val calls = AtomicInteger()

        repeat(3) {
            assertEquals("결과", cache.getOrCompute("k") { calls.incrementAndGet(); "결과" })
        }
        assertEquals(1, calls.get())
    }

    @Test
    fun `키가 다르면 각각 계산한다`() {
        val cache = StatsQueryCache()
        assertEquals(1, cache.getOrCompute("a") { 1 })
        assertEquals(2, cache.getOrCompute("b") { 2 })
        assertEquals(1, cache.getOrCompute("a") { 99 })
    }

    @Test
    fun `null 결과도 캐시해서 다시 계산하지 않는다`() {
        val cache = StatsQueryCache()
        val calls = AtomicInteger()
        repeat(2) { cache.getOrCompute<String?>("nullable") { calls.incrementAndGet(); null } }
        assertEquals(1, calls.get())
    }

    @Test
    fun `evictAll 이후에는 다시 계산한다`() {
        val cache = StatsQueryCache()
        val calls = AtomicInteger()
        cache.getOrCompute("k") { calls.incrementAndGet() }
        cache.evictAll()
        cache.getOrCompute("k") { calls.incrementAndGet() }
        assertEquals(2, calls.get())
    }

    @Test
    fun `evictByPrefix 는 접두사가 맞는 키만 지운다`() {
        val cache = StatsQueryCache()
        cache.getOrCompute("champion-stats:Ahri:all") { 1 }
        cache.getOrCompute("champion-stats:Zed:all") { 2 }
        cache.getOrCompute("player-stats:나#KR1") { 3 }

        cache.evictByPrefix("champion-stats:")

        assertEquals(10, cache.getOrCompute("champion-stats:Ahri:all") { 10 })
        assertEquals(20, cache.getOrCompute("champion-stats:Zed:all") { 20 })
        // 접두사가 다른 키는 그대로 살아 있어야 한다.
        assertEquals(3, cache.getOrCompute("player-stats:나#KR1") { 30 })
    }

    /**
     * 캐시가 빈 키에 요청이 겹치면 예전에는 전부 각자 풀스캔을 돌았다.
     * 통계 계산 한 번이 수백 ms 라 동시 진입이 그대로 DB 부하로 갔다.
     */
    @Test
    fun `같은 키에 동시에 들어와도 계산은 한 번만 돈다`() {
        val cache = StatsQueryCache()
        val threads = 16
        val calls = AtomicInteger()
        val ready = CountDownLatch(threads)
        val go = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)

        try {
            val futures = (1..threads).map {
                pool.submit<String> {
                    ready.countDown()
                    go.await()
                    cache.getOrCompute("hot") {
                        calls.incrementAndGet()
                        Thread.sleep(50) // 실제 집계처럼 느리게
                        "값"
                    }
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            go.countDown()
            futures.forEach { assertEquals("값", it.get(5, TimeUnit.SECONDS)) }
        } finally {
            pool.shutdownNow()
        }

        assertEquals(1, calls.get())
    }

    /**
     * 키에 파라미터가 붙는 통계(챔피언 상성 등)는 조합이 수만 개까지 벌어진다.
     * 상한 없이 담아 두면 만료된 항목이 evict 전까지 그대로 남는다.
     */
    @Test
    fun `키가 아무리 늘어도 상한을 넘겨 쌓이지 않는다`() {
        val cache = StatsQueryCache()
        repeat(12_000) { i -> cache.getOrCompute("champion-matchup:$i") { i } }
        assertTrue(cache.size() <= 10_000, "상한을 넘어 ${cache.size()}개가 남았다")
    }
}

/** 테스트에서만 쓰는 크기 확인. 캐시 내부 맵을 외부에 열지 않으려고 확장으로 둔다. */
private fun StatsQueryCache.size(): Int {
    val f = StatsQueryCache::class.java.getDeclaredField("store")
    f.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return (f.get(this) as Map<*, *>).size
}
