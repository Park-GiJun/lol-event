package com.gijun.main.infrastructure.config

import com.gijun.main.domain.service.RiotIdNormalizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * `riot-id.aliases` — 별명(부계정·개명 전 이름) -> 정규 이름.
 *
 * riotId 에는 `#` 이 들어가서 YAML 맵 키로 그대로 쓰면 주석으로 잘린다.
 * Spring Boot 의 대괄호 표기로 키를 원문 그대로 보존한다:
 *
 * ```yaml
 * riot-id:
 *   aliases:
 *     "[달렸노#KR1]": "qkzxfh#KR1"
 * ```
 */
@ConfigurationProperties(prefix = "riot-id")
data class RiotIdProperties(
    val aliases: Map<String, String> = emptyMap(),
)

@Configuration
@EnableConfigurationProperties(RiotIdProperties::class)
class RiotIdConfig {
    @Bean
    fun riotIdNormalizer(props: RiotIdProperties) = RiotIdNormalizer(props.aliases)
}
