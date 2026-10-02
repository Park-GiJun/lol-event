package com.gijun.main.shared.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Spring Framework 7 API versioning + 인터셉터 등록.
 *
 * 클라이언트는 `X-API-Version: 1.0` 으로 버전을 명시한다. 헤더가 없으면 기본 `1.0` 으로 매칭되므로
 * (`setDefaultVersion` + `setVersionRequired(false)`) 헤더 없는 호출도 그대로 돈다 — 이미 배포된
 * 데스크탑 수집기는 이 헤더를 보내지 않는다.
 *
 * 새 버전을 추가할 때: [addSupportedVersions] 에 `"2.0"` 을 넣고, 해당 엔드포인트만
 * `@RequestMapping(version = "2.0")` 으로 분기한 뒤, 점진 전환이 끝나면 기본값을 올린다.
 */
@Configuration
class ApiVersionConfig(
    private val apiAccessLoggingInterceptor: ApiAccessLoggingInterceptor,
) : WebMvcConfigurer {
    override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
        configurer
            .useRequestHeader(HEADER)
            .addSupportedVersions(V1)
            .setDefaultVersion(V1)
            .setVersionRequired(false)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(apiAccessLoggingInterceptor)
    }

    companion object {
        const val HEADER = "X-API-Version"
        const val V1 = "1.0"
    }
}
