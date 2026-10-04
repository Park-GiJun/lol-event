package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.AllyChampionStat
import com.gijun.main.application.dto.result.AllyPickStat
import com.gijun.main.application.dto.result.ChampionLaneStrength
import com.gijun.main.application.dto.result.LaneGap
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PickRecommendationWriterTest {
    private val names = ChampionNames(mapOf("Sion" to "사이온", "Lulu" to "룰루", "Yunara" to "유나라", "Malzahar" to "말자하", "Sylas" to "사일러스"))
    private val allies = listOf("Sion", "Lulu", "Yunara")

    @Test
    fun `아군별 기록과 그 라인 전체 성적을 같이 적는다`() {
        val malzahar = pick("Malzahar", AllyChampionStat("Lulu", 5, 3, 60), AllyChampionStat("Sion", 4, 3, 75))
        val laneStats = mapOf("Malzahar" to lane("Malzahar", 16, 10))

        val text = RagDocumentWriter.pickRecommendation("MID", allies, listOf(malzahar), laneStats, names)

        val lines = text.lines()
        assertTrue(lines.first() == "[픽 추천] 미드 — 아군: 사이온(Sion), 룰루(Lulu), 유나라(Yunara)") { text }
        assertTrue(
            lines.contains(
                "1위 말자하(Malzahar) — 아군과 합쳐 9판 6승, 승률 66% (룰루(Lulu)와 5판 3승, 사이온(Sion)와 4판 3승). 미드 전체 16판 승률 62%.",
            ),
        ) { text }
        assertTrue(lines.last() == "위 후보들과 같이 한 기록이 없는 아군: 유나라(Yunara).") { text }
    }

    @Test
    fun `표본이 모자란 후보는 적지 않는다`() {
        val malzahar = pick("Malzahar", AllyChampionStat("Lulu", 5, 3, 60))
        val sylas = pick("Sylas", AllyChampionStat("Sion", 2, 2, 100))

        val text = RagDocumentWriter.pickRecommendation("MID", allies, listOf(sylas, malzahar), emptyMap(), names)

        assertFalse(text.contains("사일러스")) { text }
        assertTrue(text.contains("1위 말자하(Malzahar)")) { text }
    }

    @Test
    fun `추천할 기록이 없으면 라인 전체 순위로 답하라고 말한다`() {
        val text = RagDocumentWriter.pickRecommendation("MID", allies, emptyList(), emptyMap(), names)

        assertTrue(text.contains("3판 이상 나온 미드 챔피언이 없다")) { text }
        assertTrue(text.contains("get_lane_champions")) { text }
    }

    private fun pick(
        champion: String,
        vararg withAllies: AllyChampionStat,
    ): AllyPickStat {
        val games = withAllies.sumOf { it.games }
        val wins = withAllies.sumOf { it.wins }
        return AllyPickStat(champion, withAllies.toList(), games, wins, wins * 100 / games, 0.0)
    }

    private fun lane(
        champion: String,
        games: Int,
        wins: Int,
    ) = ChampionLaneStrength(champion, 0, "MID", games, wins, wins * 100 / games, 0.0, "MEDIUM", LaneGap(0, 0.0, 0, 0.0, 0.0))
}
