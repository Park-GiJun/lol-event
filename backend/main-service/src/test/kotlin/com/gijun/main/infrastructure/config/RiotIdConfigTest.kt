package com.gijun.main.infrastructure.config

import com.gijun.main.domain.service.RiotIdNormalizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * 별명 표가 설정에서 제대로 실려 오는지 본다.
 *
 * riotId 에는 `#` 이 들어간다. YAML 맵 키로 그냥 쓰면 주석으로 잘리고, 따옴표만 씌우면
 * Spring 의 느슨한 바인딩이 키를 건드린다. 대괄호 표기가 그 둘을 모두 피하는 유일한 방법인데,
 * 이건 문서를 읽어서 아는 것일 뿐 컴파일러가 잡아 주지 않는다. 그래서 테스트로 못 박는다.
 *
 * DB 도 웹 서버도 띄우지 않는다 — 설정 바인딩 하나만 보는 테스트다.
 */
class RiotIdConfigTest {

    private val runner = ApplicationContextRunner()
        // @ConfigurationProperties 를 실제로 바인딩해 주는 후처리기. 자동 설정을 통째로 켜지 않고
        // 이것 하나만 등록한다.
        .withInitializer { ConfigurationPropertiesBindingPostProcessor.register(it.beanFactory as BeanDefinitionRegistry) }
        .withUserConfiguration(RiotIdConfig::class.java)

    @Test
    fun `대괄호 표기로 준 별명 키는 원문 그대로 실린다`() {
        runner
            .withPropertyValues("riot-id.aliases[달렸노#KR1]=qkzxfh#KR1")
            .run { context ->
                val normalizer = context.getBean(RiotIdNormalizer::class.java)
                assertEquals("qkzxfh#KR1", normalizer.canonical("달렸노#KR1"))
                assertEquals("qkzxfh#KR1", normalizer.canonical("qkzxfh#KR1"))
                assertEquals("남#KR1", normalizer.canonical("남#KR1"))
            }
    }

    @Test
    fun `표가 없어도 기동한다`() {
        runner.run { context ->
            assertEquals("아무개#KR1", context.getBean(RiotIdNormalizer::class.java).canonical("아무개#KR1"))
        }
    }
}
