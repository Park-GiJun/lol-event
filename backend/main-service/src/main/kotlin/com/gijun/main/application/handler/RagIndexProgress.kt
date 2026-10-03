package com.gijun.main.application.handler

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * 전체 색인이 어디까지 갔는지. 색인은 뒤에서 돌고 화면은 이걸 물어본다.
 *
 * 메모리에만 둔다. 서비스가 다시 뜨면 "돈 적 없음" 으로 돌아가는데, 저장된 문서 수는 DB 에서
 * 따로 세므로 그걸로 충분하다.
 */
@Component
class RagIndexProgress {
    data class Snapshot(
        val running: Boolean = false,
        val total: Int = 0,
        val processed: Int = 0,
        val embedded: Int = 0,
        val failed: Int = 0,
        val startedAt: Instant? = null,
        val finishedAt: Instant? = null,
        val lastError: String? = null,
    )

    private val state = AtomicReference(Snapshot())

    fun snapshot(): Snapshot = state.get()

    /** @return 이미 돌고 있으면 false. */
    fun tryStart(): Boolean {
        while (true) {
            val current = state.get()
            if (current.running) return false
            if (state.compareAndSet(current, Snapshot(running = true, startedAt = Instant.now()))) return true
        }
    }

    fun total(total: Int) = state.updateAndGet { it.copy(total = total) }

    fun recorded(embedded: Boolean) =
        state.updateAndGet { it.copy(processed = it.processed + 1, embedded = it.embedded + if (embedded) 1 else 0) }

    fun failed(reason: String) = state.updateAndGet { it.copy(processed = it.processed + 1, failed = it.failed + 1, lastError = reason) }

    fun finish(error: String? = null) =
        state.updateAndGet { it.copy(running = false, finishedAt = Instant.now(), lastError = error ?: it.lastError) }
}
