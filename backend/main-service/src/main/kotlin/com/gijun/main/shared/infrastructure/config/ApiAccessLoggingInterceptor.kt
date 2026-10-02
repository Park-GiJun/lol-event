package com.gijun.main.shared.infrastructure.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * 요청 한 건을 한 줄로 남긴다 — `GET /api/stats/elo -> 200 (37ms)`.
 *
 * 등록은 [ApiVersionConfig.addInterceptors] 가 한다.
 *
 * 이 서비스에는 로그인이 없어 요청 주체를 찍지 않는다. 인증이 생기면 여기서 MDC `userId` 를
 * 심어 그 요청 안의 모든 로그에 사용자가 붙게 한다.
 *
 * **폴링은 제외한다** — Prometheus 가 주기적으로 긁는 `/actuator` 까지 남기면 로그가 그것으로
 * 덮인다. [SILENT_POLL_PATHS] 에 경로를 추가해 늘린다.
 */
@Component
class ApiAccessLoggingInterceptor : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        request.setAttribute(STARTED_AT, System.currentTimeMillis())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        if (isSilentPoll(request)) return
        val startedAt = request.getAttribute(STARTED_AT) as? Long
        val elapsed = startedAt?.let { System.currentTimeMillis() - it } ?: -1
        log.info("{} {} -> {} ({}ms)", request.method, request.requestURI, response.status, elapsed)
    }

    private fun isSilentPoll(request: HttpServletRequest): Boolean = SILENT_POLL_PATHS.any { request.requestURI.startsWith(it) }

    private companion object {
        private const val STARTED_AT = "apiAccessStartedAt"

        /** 주기적으로 불리는 것들. 늘어나면 여기에 추가한다. */
        private val SILENT_POLL_PATHS = listOf("/actuator")
    }
}
