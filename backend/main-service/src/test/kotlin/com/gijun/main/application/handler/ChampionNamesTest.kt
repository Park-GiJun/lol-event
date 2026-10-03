package com.gijun.main.application.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChampionNamesTest {
    private val names = ChampionNames(mapOf("Seraphine" to "세라핀", "MissFortune" to "미스 포츈", "KogMaw" to "코그모", "Chogath" to "초가스"))

    @Test
    fun `한글 이름과 영문 키를 같이 적는다`() {
        assertEquals("세라핀(Seraphine)", names.label("Seraphine"))
        // Data Dragon 에 없는 키는 그대로 쓴다. 문서가 비는 것보다 낫다.
        assertEquals("NewChamp", names.label("NewChamp"))
    }

    @Test
    fun `사람이 쓰는 대로 받아 영문 키를 찾는다`() {
        assertEquals("Seraphine", names.resolve("세라핀"))
        assertEquals("Seraphine", names.resolve("seraphine"))
        assertEquals("MissFortune", names.resolve("미스포츈"))
        assertEquals("MissFortune", names.resolve("miss fortune"))
        assertEquals("KogMaw", names.resolve("Kog'Maw"))
    }

    @Test
    fun `정확히 맞는 이름이 일부만 맞는 이름보다 먼저다`() {
        val overlapping = ChampionNames(mapOf("Leona" to "레오나", "Leo" to "레오"))

        assertEquals("Leo", overlapping.resolve("레오"))
    }

    @Test
    fun `없는 이름은 지어내지 않는다`() {
        assertNull(names.resolve("없는챔피언"))
        assertNull(names.resolve("  "))
    }
}
