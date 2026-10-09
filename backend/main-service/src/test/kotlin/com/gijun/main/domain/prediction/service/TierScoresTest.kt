package com.gijun.main.domain.prediction.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 기댓값은 `lol-ml` 의 `TierPriorTest` 와 같다. */
class TierScoresTest {
    @Test
    fun `한 티어에 1 이고 단계와 LP 는 그 사이를 나눈다`() {
        assertEquals(0.0, TierScores.score("IRON", "IV", 0), 1e-12)
        assertEquals(3.0, TierScores.score("GOLD", "IV", 0), 1e-12)
        assertEquals(4.625, TierScores.score("PLATINUM", "II", 50), 1e-12)
        // 다이아 I 100LP 는 마스터 0LP 와 같은 자리다.
        assertEquals(TierScores.score("MASTER", "I", 0), TierScores.score("DIAMOND", "I", 100), 1e-12)
        assertEquals(8.0, TierScores.score("GRANDMASTER", "I", 400), 1e-12)
    }

    @Test
    fun `점수를 다시 티어 말로 옮긴다`() {
        assertEquals("PLATINUM II", TierScores.label(4.625))
        assertEquals("GOLD IV", TierScores.label(3.0))
        assertEquals("DIAMOND I", TierScores.label(6.99))
        assertEquals("MASTER+", TierScores.label(7.3))
        assertEquals("IRON IV", TierScores.label(-0.2))
    }
}
