package com.gijun.main.shared.infrastructure.web.common

import com.gijun.main.domain.member.exception.MemberAlreadyExistsException
import com.gijun.main.shared.infrastructure.config.ApiAccessLoggingInterceptor
import com.gijun.main.shared.infrastructure.config.ApiVersionConfig
import com.gijun.main.shared.infrastructure.config.SecurityConfig
import com.gijun.main.shared.infrastructure.config.TraceIdFilter
import com.gijun.main.shared.infrastructure.config.WebConfig
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 봉투와 상태코드가 **실제 MVC 파이프라인에서** 어떻게 나가는지 본다.
 *
 * 매핑 전수성은 `GlobalExceptionHandlerMappingTest` 가 반사로 잡지만, 그것만으로는 "catch-all 보다
 * 먼저 매칭되는가" 와 "직렬화된 모양이 계약대로인가" 를 알 수 없다. 둘 다 수집기와 화면이 기대는
 * 값이라 여기서 못박는다.
 */
@WebMvcTest(GlobalExceptionHandlerWebTest.ProbeWebAdapter::class)
@Import(
    GlobalExceptionHandler::class,
    GlobalExceptionHandlerWebTest.ProbeConfig::class,
    ApiVersionConfig::class,
    ApiAccessLoggingInterceptor::class,
    TraceIdFilter::class,
    SecurityConfig::class,
    WebConfig::class,
)
class GlobalExceptionHandlerWebTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `성공 응답은 봉투에 싸이고 timestamp 는 ISO 문자열이다`() {
        mockMvc
            .get("/probe/ok") { param("name", "faker") }
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { value("faker") }
                // 숫자(epoch)로 나가면 예전 응답과 타입이 달라진다.
                jsonPath("$.timestamp") { value(matchesPattern("""\d{4}-\d{2}-\d{2}T.*Z""")) }
            }
    }

    @Test
    fun `도메인 예외는 카테고리의 상태코드와 자기 errorCode 로 나간다`() {
        mockMvc
            .get("/probe/duplicate")
            .andExpect {
                status { isConflict() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.errorCode") { value("DUPLICATE_MEMBER") }
            }
    }

    @Test
    fun `필수 파라미터 누락은 500 이 아니라 400 이다`() {
        mockMvc
            .get("/probe/ok")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("MISSING_PARAMETER") }
            }
    }

    @Test
    fun `없는 경로는 500 이 아니라 404 다`() {
        mockMvc
            .get("/probe/nowhere")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("NOT_FOUND") }
            }
    }

    @Test
    fun `메서드 불일치는 500 이 아니라 405 다`() {
        mockMvc
            .post("/probe/ok")
            .andExpect {
                status { isMethodNotAllowed() }
                jsonPath("$.errorCode") { value("METHOD_NOT_ALLOWED") }
            }
    }

    @Test
    fun `예상하지 못한 예외는 원문을 숨기고 500 으로 나간다`() {
        mockMvc
            .get("/probe/boom")
            .andExpect {
                status { isInternalServerError() }
                jsonPath("$.errorCode") { value("INTERNAL_ERROR") }
                jsonPath("$.message") { value("서버 오류가 발생했다.") }
            }
    }

    @Test
    fun `버전 헤더가 없어도 버전이 붙은 매핑에 닿는다`() {
        // 이미 배포된 수집기는 X-API-Version 을 보내지 않는다.
        mockMvc.get("/probe/versioned").andExpect { status { isOk() } }
        mockMvc
            .get("/probe/versioned") { header(ApiVersionConfig.HEADER, ApiVersionConfig.V1) }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `추적 ID 는 받은 값을 그대로 돌려주고 없으면 만든다`() {
        mockMvc
            .get("/probe/ok") {
                param("name", "x")
                header(TraceIdFilter.HEADER, "abc12345")
            }.andExpect { header { string(TraceIdFilter.HEADER, "abc12345") } }
        mockMvc
            .get("/probe/ok") { param("name", "x") }
            .andExpect { header { exists(TraceIdFilter.HEADER) } }
    }

    @RestController
    @RequestMapping("/probe")
    class ProbeWebAdapter {
        @GetMapping("/ok")
        fun ok(
            @RequestParam name: String,
        ): CommonApiResponse<String> = CommonApiResponse.success(name)

        @GetMapping("/duplicate")
        fun duplicate(): CommonApiResponse<Unit> = throw MemberAlreadyExistsException("Faker#KR1")

        @GetMapping("/boom")
        fun boom(): CommonApiResponse<Unit> = error("DB 접속 정보가 들어 있을 수 있는 원문")

        @GetMapping("/versioned", version = "1.0")
        fun versioned(): CommonApiResponse<Unit> = CommonApiResponse.success(Unit)
    }

    @TestConfiguration
    class ProbeConfig {
        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
    }
}
