package com.gijun.main.infrastructure.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("LoL 내전 이벤트 API")
                .description("League of Legends 내전 경기 수집 및 통계 서비스")
                .version("v1.0.0")
                .contact(Contact().name("lol-event").url("https://github.com/Park-GiJun/lol-event"))
        )
        .servers(
            listOf(
                Server().url("/").description("현재 서버"),
            )
        )
        .components(Components())
}
