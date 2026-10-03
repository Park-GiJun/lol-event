package com.gijun.main.infrastructure.adapter.out.external

import com.gijun.main.domain.rag.exception.RagBusyException
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * LLM 서버로 가는 문.
 *
 * llama-server 는 슬롯이 하나라 한 번에 한 답만 만든다. 요청을 그냥 다 보내면 뒤에 온 것은 서버
 * 안에서 줄을 서고, 그동안 이쪽 요청 스레드(톰캣)가 하나씩 묶인다. 줄이 길어지면 통계 API 까지
 * 느려지므로, 조금만 기다려 보고 안 되면 "바쁘다" 로 돌려보낸다.
 */
@Component
class LlmGate {
    private val permits = Semaphore(CONCURRENT, true)

    fun <T> enter(block: () -> T): T {
        if (!permits.tryAcquire(WAIT_SECONDS, TimeUnit.SECONDS)) throw RagBusyException()
        try {
            return block()
        } finally {
            permits.release()
        }
    }

    private companion object {
        /** 하나는 답을 만들고 하나는 그 뒤에서 기다린다. */
        const val CONCURRENT = 2
        const val WAIT_SECONDS = 25L
    }
}
