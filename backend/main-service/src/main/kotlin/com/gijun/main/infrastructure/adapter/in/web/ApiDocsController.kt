package com.gijun.main.infrastructure.adapter.`in`.web

import io.swagger.v3.oas.models.OpenAPI
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * springdoc의 /api-docs 엔드포인트가 auto-configuration 실패로 등록되지 않을 경우 fallback.
 * springdoc의 OpenApiResource 빈이 등록된 경우 이 컨트롤러는 비활성화됩니다.
 */
@RestController
@ConditionalOnMissingBean(name = ["openApiResource", "openApiWebMvcResource"])
class ApiDocsController(private val openAPI: OpenAPI) {

    @GetMapping("/api-docs", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun apiDocs(): OpenAPI = openAPI
}
