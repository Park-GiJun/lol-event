package com.gijun.main.shared.infrastructure.web.common

import com.gijun.main.shared.domain.exception.DomainException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.ExceptionHandler
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

/**
 * **[DomainException] 카테고리 전수 매핑을 강제한다.**
 *
 * [GlobalExceptionHandler] 가 개별 예외 클래스를 알지 않는 설계의 가치 전부가 이 테스트에 달려
 * 있다 — 8 번째 카테고리를 추가하면 핸들러를 고치기 전까지 red 가 되어, 새 예외가 조용히
 * 500 으로 떨어지는 일을 막는다.
 */
class GlobalExceptionHandlerMappingTest {
    private val handledTypes: Set<String> =
        GlobalExceptionHandler::class
            .functions
            .flatMap {
                it
                    .findAnnotation<ExceptionHandler>()
                    ?.value
                    .orEmpty()
                    .toList()
            }.mapNotNull { it.simpleName }
            .toSet()

    @Test
    fun `DomainException 의 모든 카테고리가 매핑돼 있다`() {
        val categories = DomainException::class.sealedSubclasses.mapNotNull { it.simpleName }

        assertTrue(categories.isNotEmpty(), "sealed 하위가 비었다 — 계층이 한 파일에 있는지 확인한다")
        val missing = categories.filterNot { it in handledTypes }
        assertTrue(missing.isEmpty(), "@ExceptionHandler 매핑이 없는 카테고리: $missing")
    }

    @Test
    fun `카테고리는 일곱이다`() {
        assertEquals(EXPECTED_CATEGORY_COUNT, DomainException::class.sealedSubclasses.size)
    }

    @Test
    fun `프레임워크 예외와 catch-all 도 매핑돼 있다`() {
        val required =
            listOf(
                "AccessDeniedException",
                "MethodArgumentNotValidException",
                "MissingServletRequestParameterException",
                "HttpMessageNotReadableException",
                // 아래 넷은 catch-all 이 삼키면 전부 500 이 된다 — Spring MVC 가 스스로 내던 상태코드다.
                "NoResourceFoundException",
                "HttpRequestMethodNotSupportedException",
                "HttpMediaTypeNotSupportedException",
                "MethodArgumentTypeMismatchException",
                "Exception",
            )

        val missing = required.filterNot { it in handledTypes }
        assertTrue(missing.isEmpty(), "매핑이 없는 프레임워크 예외: $missing")
    }

    private companion object {
        private const val EXPECTED_CATEGORY_COUNT = 7
    }
}
