package com.gijun.main.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RiotIdNormalizerTest {

    @Test
    fun `표가 비어 있으면 앞뒤 공백만 다듬는다`() {
        val n = RiotIdNormalizer(emptyMap())
        assertEquals("Dokyon#두 웅", n.canonical("  Dokyon#두 웅 "))
    }

    @Test
    fun `별명은 정규 이름으로 바뀐다`() {
        val n = RiotIdNormalizer(mapOf("달렸노#KR1" to "qkzxfh#KR1"))
        assertEquals("qkzxfh#KR1", n.canonical("달렸노#KR1"))
        assertEquals("qkzxfh#KR1", n.canonical("qkzxfh#KR1"))
    }

    @Test
    fun `조회는 대소문자와 공백을 무시한다`() {
        val n = RiotIdNormalizer(mapOf("Abc#KR1" to "정답#KR1"))
        assertEquals("정답#KR1", n.canonical(" abc#kr1 "))
    }

    @Test
    fun `체인은 끝까지 따라간다`() {
        val n = RiotIdNormalizer(mapOf("a#KR1" to "b#KR1", "b#KR1" to "c#KR1"))
        assertEquals("c#KR1", n.canonical("a#KR1"))
    }

    @Test
    fun `순환이면 멈추고 값을 돌려준다 — 설정 실수로 서버가 멈추지 않는다`() {
        val n = RiotIdNormalizer(mapOf("a#KR1" to "b#KR1", "b#KR1" to "a#KR1"))
        val result = n.canonical("a#KR1")
        assertEquals(true, result == "a#KR1" || result == "b#KR1")
    }

    @Test
    fun `같은 사람이 두 이름으로 들어오면 하나로 접힌다`() {
        val n = RiotIdNormalizer(mapOf("달렸노#KR1" to "qkzxfh#KR1"))
        assertEquals(
            listOf("qkzxfh#KR1", "다른사람#KR1"),
            n.canonicalDistinct(listOf("달렸노#KR1", "qkzxfh#KR1", "다른사람#KR1")),
        )
    }
}
