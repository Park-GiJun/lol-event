package com.gijun.lolml.extract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonTest {
    @Test
    fun `Riot API 의 티어 응답을 읽는다`() {
        val body =
            """
            [{"leagueId":"a-b","queueType":"RANKED_SOLO_5x5","tier":"EMERALD","rank":"II","leaguePoints":41,
              "wins":120,"losses":111,"veteran":false,"hotStreak":true,"miniSeries":{"target":3,"progress":"WLN"}},
             {"queueType":"RANKED_FLEX_SR","tier":"GOLD","rank":"IV","leaguePoints":0,"wins":3,"losses":2}]
            """.trimIndent()

        val entries = Json.parse(body) as List<*>

        assertEquals(2, entries.size)
        val solo = entries[0] as Map<*, *>
        assertEquals("EMERALD", solo["tier"])
        assertEquals(41.0, solo["leaguePoints"])
        assertEquals(true, solo["hotStreak"])
        assertEquals("WLN", (solo["miniSeries"] as Map<*, *>)["progress"])
    }

    @Test
    fun `빈 것과 null 과 escape 를 읽는다`() {
        assertEquals(emptyList<Any?>(), Json.parse(" [ ] "))
        assertEquals(emptyMap<String, Any?>(), Json.parse("{}"))
        assertEquals(mapOf("a" to null, "b" to -1.5e2), Json.parse("""{"a":null,"b":-1.5e2}"""))
        assertEquals("한\"글\n가", Json.parse(""""한\"글\n가""""))
    }

    @Test
    fun `깨진 JSON 은 조용히 넘어가지 않는다`() {
        assertFailsWith<IllegalArgumentException> { Json.parse("""{"a":1} x""") }
        assertFailsWith<IllegalStateException> { Json.parse("""{"a":""") }
    }
}
