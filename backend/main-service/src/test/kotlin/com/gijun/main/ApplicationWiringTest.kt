package com.gijun.main

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.util.function.Supplier

/**
 * **유즈케이스·핸들러·인바운드 어댑터가 서로 물리는지** 본다. DB·Kafka 없이 돈다.
 *
 * 핸들러 단위 테스트와 웹 슬라이스 테스트는 각자 필요한 것만 올리므로, "유즈케이스 인터페이스는
 * 있는데 구현한 핸들러가 없다" 나 "두 핸들러가 같은 유즈케이스를 구현한다" 는 기동할 때에야
 * 터진다. 핸들러를 주제별로 합치고 쪼개는 일이 잦아 그 실수를 여기서 먼저 잡는다.
 *
 * 아웃바운드 포트는 전부 목이다. 영속·외부 호출 어댑터의 연결은 이 테스트가 보지 않는다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationWiringTest {
    private val useCases = interfacesUnder("com/gijun/main/application/port/in")
    private val outPorts = interfacesUnder("com/gijun/main/application/port/out")

    private val context =
        AnnotationConfigApplicationContext().apply {
            outPorts.forEach { port -> registerMock(port) }
            registerBean("objectMapper", ObjectMapper::class.java, Supplier { ObjectMapper() })
            scan(
                "com.gijun.main.application.handler",
                "com.gijun.main.infrastructure.adapter.in",
            )
            refresh()
        }

    @AfterAll
    fun close() = context.close()

    @Test
    fun `유즈케이스마다 구현한 핸들러가 정확히 하나다`() {
        assertTrue(useCases.size > MIN_EXPECTED_USE_CASES, "유즈케이스를 못 찾았다 — 스캔 경로를 확인한다 (${useCases.size} 개)")

        val wrong =
            useCases.mapNotNull { useCase ->
                val beans = context.getBeanNamesForType(useCase)
                if (beans.size == 1) null else "${useCase.simpleName}: ${beans.toList()}"
            }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `인바운드 어댑터가 전부 뜬다`() {
        val adapters = context.beanDefinitionNames.filter { it.endsWith("WebAdapter") }
        assertTrue(adapters.size >= MIN_EXPECTED_WEB_ADAPTERS, "웹 어댑터가 덜 떴다: $adapters")
    }

    private fun <T : Any> AnnotationConfigApplicationContext.registerMock(type: Class<T>) {
        registerBean(type.simpleName, type, Supplier { Mockito.mock(type) })
    }

    private fun interfacesUnder(path: String): List<Class<*>> =
        PathMatchingResourcePatternResolver()
            .getResources("classpath*:$path/**/*.class")
            .map { it.url.path }
            .filterNot { it.contains('$') }
            .map {
                it
                    .substringAfterLast("/classes/kotlin/main/")
                    .removeSuffix(".class")
                    .replace('/', '.')
            }.filter { it.startsWith("com.gijun.main") }
            .map { Class.forName(it) }
            .filter { it.isInterface }

    private companion object {
        private const val MIN_EXPECTED_USE_CASES = 60
        private const val MIN_EXPECTED_WEB_ADAPTERS = 18
    }
}
