package com.gijun.main.infrastructure.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Jackson 2 ObjectMapper 빈.
 *
 * RiotApiAdapter / DataDragonAdapter / MatchEventConsumer 가
 * com.fasterxml.jackson.databind.ObjectMapper 를 주입받는다.
 *
 * 이 빈은 지금까지 spring-cloud-starter-config 가 딸려 오면서 같이 등록되고 있었다.
 * Spring Boot 4 는 Jackson 3(tools.jackson) 이 기본이라 Jackson 2 쪽 ObjectMapper 는
 * 자동으로 만들어 주지 않는다. Config Server 를 걷어내자 그 빈이 사라져
 * "Parameter 2 of constructor in RiotApiAdapter required a bean of type ObjectMapper"
 * 로 기동이 막혔다.
 *
 * 라이브러리에 묻어 오던 것을 여기서 명시적으로 만든다.
 */
@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        // Riot / DataDragon 응답은 필드가 수시로 늘어난다. 모르는 필드에 터지면 안 된다.
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
