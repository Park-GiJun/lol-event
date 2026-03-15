package com.gijun.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RouteConfig(
    @Value("\${lcu.service.url}") private val lcuServiceUrl: String
) {

    @Bean
    fun routeLocator(builder: RouteLocatorBuilder): RouteLocator =
        builder.routes()
            .route("lcu-service") { r ->
                r.path("/api/lcu/**")
                    .filters { f -> f.removeRequestHeader("Origin") }
                    .uri(lcuServiceUrl)
            }
            .route("main-service") { r ->
                r.path("/api/**")
                    .filters { f -> f.removeRequestHeader("Origin") }
                    .uri("lb://main-service")
            }
            .build()
}
