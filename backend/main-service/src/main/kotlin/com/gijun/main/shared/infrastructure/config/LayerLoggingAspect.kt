package com.gijun.main.shared.infrastructure.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 어댑터·핸들러 경계를 한 줄씩 찍는다 — `ClassName.method 12ms`, 실패면 `... FAIL ExceptionName`.
 *
 * **local 프로파일 전용**이다. 화면 하나가 어댑터·핸들러를 수십 번 오가므로 운영에서 켜면 로그가
 * 실행 흐름에 묻힌다. 운영 추적은 [ApiAccessLoggingInterceptor] 의 요청 한 줄과
 * `GlobalExceptionHandler` 의 예외 로그가 맡는다.
 */
@Aspect
@Component
@Profile("local")
class LayerLoggingAspect {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around(
        "execution(* com.gijun.main..infrastructure.adapter..*Adapter.*(..)) || " +
            "execution(* com.gijun.main..application.handler..*Handler.*(..))",
    )
    fun logLayer(joinPoint: ProceedingJoinPoint): Any? {
        val target = "${joinPoint.signature.declaringType.simpleName}.${joinPoint.signature.name}"
        val startedAt = System.currentTimeMillis()
        return try {
            joinPoint.proceed().also { log.debug("{} {}ms", target, System.currentTimeMillis() - startedAt) }
        } catch (e: Throwable) {
            log.debug("{} {}ms FAIL {}", target, System.currentTimeMillis() - startedAt, e.javaClass.simpleName)
            throw e
        }
    }
}
