package com.gijun.main.infrastructure.adapter.out.external

import com.gijun.main.domain.rag.exception.RagBusyException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LlmGateTest {
    @Test
    fun `자리가 차면 기다리지 않고 바로 거절한다`() {
        val gate = LlmGate()
        val inside = CountDownLatch(3)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(3)
        try {
            repeat(3) {
                pool.execute {
                    gate.enter {
                        inside.countDown()
                        release.await()
                    }
                }
            }
            assertTrue(inside.await(5, TimeUnit.SECONDS))

            // 기다리게 하면 기다리는 요청이 톰캣 스레드를 잡는다. 문을 둔 뜻이 없어진다.
            val startedAt = System.nanoTime()
            assertThrows(RagBusyException::class.java) { gate.enter { } }
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt) < 1_000)
        } finally {
            release.countDown()
            pool.shutdown()
        }
    }

    @Test
    fun `안에서 실패해도 자리를 돌려준다`() {
        val gate = LlmGate()

        repeat(10) { assertThrows(IllegalStateException::class.java) { gate.enter { error("LLM 실패") } } }

        assertEquals("ok", gate.enter { "ok" })
    }
}
