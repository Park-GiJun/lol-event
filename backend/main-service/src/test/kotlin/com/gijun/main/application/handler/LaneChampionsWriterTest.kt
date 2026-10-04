package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.ChampionLaneStrength
import com.gijun.main.application.dto.result.LaneGap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LaneChampionsWriterTest {
    private val names = ChampionNames(mapOf("Ahri" to "아리", "Syndra" to "신드라", "Zed" to "제드"))

    @Test
    fun `준 순서대로 순위를 매기고 한글 이름을 같이 적는다`() {
        val text = RagDocumentWriter.laneChampions("MID", listOf(entry("Ahri", 12, 8), entry("Syndra", 5, 3)), names)

        val lines = text.lines()
        assertTrue(lines.first().startsWith("[라인] 미드 챔피언 순위")) { text }
        assertTrue(lines.contains("1위 아리(Ahri) — 12판 8승, 승률 66%")) { text }
        assertTrue(lines.contains("2위 신드라(Syndra) — 5판 3승, 승률 60%")) { text }
    }

    @Test
    fun `표본이 모자란 챔피언은 이름을 적지 않고 수만 센다`() {
        val text = RagDocumentWriter.laneChampions("MID", listOf(entry("Ahri", 12, 8), entry("Zed", 1, 1)), names)

        assertFalse(text.contains("제드")) { text }
        assertTrue(text.contains("3판 미만이라 뺀 챔피언 1종")) { text }
    }

    @Test
    fun `순위를 매길 표본이 없으면 그렇게 말한다`() {
        assertEquals("미드 라인 기록이 없다.", RagDocumentWriter.laneChampions("MID", emptyList(), names))
        assertEquals(
            "미드에서 3판 이상 나온 챔피언이 없어 순위를 매길 수 없다.",
            RagDocumentWriter.laneChampions("MID", listOf(entry("Zed", 2, 2)), names),
        )
    }

    private fun entry(
        champion: String,
        games: Int,
        wins: Int,
    ) = ChampionLaneStrength(
        champion = champion,
        championId = 0,
        position = "MID",
        games = games,
        wins = wins,
        winRate = wins * 100 / games,
        adjustedWinRate = 0.0,
        sampleGrade = "MEDIUM",
        gap = LaneGap(0, 0.0, 0, 0.0, 0.0),
    )
}
