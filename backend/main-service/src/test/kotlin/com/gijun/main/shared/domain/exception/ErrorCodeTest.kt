package com.gijun.main.shared.domain.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [ErrorCode] 의 **이름 드리프트와 형태 붕괴를 막는다.**
 *
 * 공통 블록의 이름은 프레임워크 예외 매핑과 화면 쪽 문구 표가 같이 기대는 값이다. 한쪽에서
 * `NOT_FOUND` 를 `RESOURCE_NOT_FOUND` 로 바꾸면 아무것도 깨지지 않은 채 화면 분기만 조용히 죽는다.
 */
class ErrorCodeTest {
    @Test
    fun `공통 코드가 모두 있고 이름이 정확하다`() {
        val expected =
            listOf(
                "NOT_FOUND",
                "CONFLICT",
                "VALIDATION_FAILED",
                "MISSING_PARAMETER",
                "MALFORMED_BODY",
                "METHOD_NOT_ALLOWED",
                "UNSUPPORTED_MEDIA_TYPE",
                "TYPE_MISMATCH",
                "INTERNAL_ERROR",
            )

        assertEquals(expected, ErrorCode.COMMON.map { it.name })
    }

    @Test
    fun `이름은 SCREAMING_SNAKE_CASE 다`() {
        // 화면 쪽 생성물이 이 이름을 그대로 TS 식별자로 쓴다 — 소문자·하이픈이 섞이면 생성물이 깨진다.
        val pattern = Regex("^[A-Z][A-Z0-9_]*$")
        val violations = ErrorCode.entries.map { it.name }.filterNot { pattern.matches(it) }
        assertTrue(violations.isEmpty(), "이름 규칙 위반: $violations")
    }

    @Test
    fun `모든 코드에 설명이 있다`() {
        val blank = ErrorCode.entries.filter { it.description.isBlank() }.map { it.name }
        assertTrue(blank.isEmpty(), "설명이 빈 코드: $blank")
    }
}
