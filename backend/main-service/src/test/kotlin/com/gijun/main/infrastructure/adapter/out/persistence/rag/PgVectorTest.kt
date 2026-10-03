package com.gijun.main.infrastructure.adapter.out.persistence.rag

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PgVectorTest {
    @Test
    fun `대괄호 안에 쉼표로 이어 쓴다`() {
        assertEquals("[0.5,-1.25,3.0]", PgVector.literal(listOf(0.5f, -1.25f, 3f)))
    }

    @Test
    fun `아주 작은 값의 지수 표기도 pgvector 가 읽는 모양이다`() {
        // Kotlin 은 1e-5 를 "1.0E-5" 로 쓴다. pgvector 의 입력은 C 의 strtof 라 이 표기를 읽는다.
        assertEquals("[1.0E-5]", PgVector.literal(listOf(0.00001f)))
    }

    @Test
    fun `빈 벡터와 숫자가 아닌 값은 DB 에 보내기 전에 막는다`() {
        assertThrows(IllegalArgumentException::class.java) { PgVector.literal(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { PgVector.literal(listOf(Float.NaN)) }
        assertThrows(IllegalArgumentException::class.java) { PgVector.literal(listOf(Float.POSITIVE_INFINITY)) }
    }
}
