package com.gijun.main.infrastructure.adapter.out.external

import com.gijun.main.domain.rag.exception.RagBusyException
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore

/**
 * LLM 서버로 가는 문.
 *
 * llama-server 는 슬롯이 하나라 한 번에 한 답만 만들고, 나머지는 서버 안에서 줄을 선다. 그동안
 * 이쪽 요청 스레드(톰캣)가 하나씩 묶인다. 이 API 는 인증이 없어서, 문이 없으면 질문을 200 개 던지는
 * 것만으로 톰캣 스레드를 다 잡아 통계 API 까지 멈출 수 있다.
 *
 * **자리가 없으면 기다리지 않고 바로 돌려보낸다.** 여기서 기다리게 하면 기다리는 요청이 똑같이
 * 스레드를 잡으므로 막는 것이 없다. 그래서 LLM 때문에 묶이는 스레드는 많아야 [IN_FLIGHT] 개다.
 */
@Component
class LlmGate {
    private val permits = Semaphore(IN_FLIGHT)

    fun <T> enter(block: () -> T): T {
        if (!permits.tryAcquire()) throw RagBusyException()
        try {
            return block()
        } finally {
            permits.release()
        }
    }

    private companion object {
        /** 하나는 답을 만들고, 둘은 서버 안에서 차례를 기다린다. */
        const val IN_FLIGHT = 3
    }
}
