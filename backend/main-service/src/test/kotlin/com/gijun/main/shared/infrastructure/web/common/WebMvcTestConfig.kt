package com.gijun.main.shared.infrastructure.web.common

import com.gijun.main.shared.infrastructure.config.ApiAccessLoggingInterceptor
import com.gijun.main.shared.infrastructure.config.ApiVersionConfig
import com.gijun.main.shared.infrastructure.config.GameModeConverter
import com.gijun.main.shared.infrastructure.config.SecurityConfig
import com.gijun.main.shared.infrastructure.config.TraceIdFilter
import com.gijun.main.shared.infrastructure.config.WebConfig
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

/**
 * 웹 어댑터 슬라이스 테스트가 공통으로 올리는 것들.
 *
 * `@WebMvcTest` 는 대상 어댑터만 올린다. 예외 봉투·버전 헤더·추적 ID·모드 변환기는 실제 요청이
 * 반드시 거치는 자리라, 빼고 테스트하면 "어댑터는 맞는데 운영에서는 400" 이 나온다.
 */
@TestConfiguration
@Import(
    GlobalExceptionHandler::class,
    ApiVersionConfig::class,
    ApiAccessLoggingInterceptor::class,
    TraceIdFilter::class,
    SecurityConfig::class,
    WebConfig::class,
    GameModeConverter::class,
)
class WebMvcTestConfig {
    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
}
