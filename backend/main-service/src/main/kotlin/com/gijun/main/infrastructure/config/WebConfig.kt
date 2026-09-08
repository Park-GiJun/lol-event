package com.gijun.main.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * CORS 설정.
 *
 * 예전에는 API Gateway 가 붙여 주고 main-service 는 `cors { disable() }` 였다.
 * 게이트웨이를 걷어내면서 브라우저가 main-service 를 직접 때리게 되므로 여기로 옮겼다.
 * 허용 오리진 목록은 게이트웨이가 쓰던 것을 그대로 가져왔다.
 *
 * 오리진은 환경변수로 뺐다. 배포 도메인이 바뀔 때 재빌드하지 않아도 되게.
 */
@Configuration
class WebConfig(
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: List<String>,
) : WebMvcConfigurer {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            this.allowedOrigins = this@WebConfig.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
