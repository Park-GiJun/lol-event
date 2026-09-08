package com.gijun.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RouteConfig(
    @Value("\${lcu.service.url}") private val lcuServiceUrl: String,
    @Value("\${grafana.url}") private val grafanaUrl: String,
    @Value("\${prometheus.url}") private val prometheusUrl: String
) {

    @Bean
    fun routeLocator(builder: RouteLocatorBuilder): RouteLocator =
        builder.routes()
            // Grafana: subpath 설정으로 prefix 유지
            .route("grafana") { r ->
                r.path("/grafana/**")
                    .filters { f -> f.removeRequestHeader("Origin") }
                    .uri(grafanaUrl)
            }
            // Prometheus: /prometheus prefix를 제거하고 전달
            .route("prometheus") { r ->
                r.path("/prometheus/**")
                    .filters { f ->
                        f.stripPrefix(1)
                            .removeRequestHeader("Origin")
                    }
                    .uri(prometheusUrl)
            }
            .route("lcu-service") { r ->
                r.path("/api/lcu/**")
                    .filters { f -> f.removeRequestHeader("Origin") }
                    .uri(lcuServiceUrl)
            }
            // Swagger UI + OpenAPI 스펙: main-service가 /api/** 밖의 경로로 서빙하므로 별도 라우트가 필요하다
            .route("main-service-docs") { r ->
                r.path("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**")
                    .filters { f -> f.removeRequestHeader("Origin") }
                    .uri("lb://main-service")
            }
            .route("main-service") { r ->
                r.path("/api/**")
                    .filters { f -> f.removeRequestHeader("Origin") }
                    .uri("lb://main-service")
            }
            .build()
}
