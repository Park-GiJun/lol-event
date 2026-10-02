package com.gijun.main.shared.infrastructure.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * 요청 하나를 로그에서 꿰기 위한 추적 ID.
 *
 * 호출자가 `X-Request-Id` 를 주면 그것을 쓰고, 없으면 UUID 앞 8 자를 만든다. MDC `traceId` 에
 * 심어 로그 패턴이 집어 가게 하고, 응답 헤더로 되돌려 호출자가 같은 키로 추적하게 한다.
 *
 * `@Order(HIGHEST_PRECEDENCE)` 인 이유는 **다른 필터가 던지는 예외까지 이 ID 아래 찍히게**
 * 하려는 것이다. 뒤에 두면 이른 예외가 추적 ID 없이 남는다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(HEADER)?.takeIf { it.isNotBlank() } ?: newTraceId()
        MDC.put(KEY, traceId)
        response.setHeader(HEADER, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(KEY)
        }
    }

    private fun newTraceId(): String = UUID.randomUUID().toString().take(LENGTH)

    companion object {
        const val HEADER = "X-Request-Id"
        const val KEY = "traceId"
        private const val LENGTH = 8
    }
}
