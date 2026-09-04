package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PositionDetectorTest {

    private val SMITE = 11

    /** 역할 전용 장화 ID. 포지션 퀘스트로 라인마다 하나씩 붙는다. */
    private val TOP_BOOTS = 1221
    private val JUNGLE_BOOTS = 1209
    private val MID_BOOTS = 1206
    private val SUPPORT_BOOTS = 1208
    private val PLAIN_BOOTS = 3006   // 버서커의 신발 — 원딜은 전용 장화가 없다

    private fun p(
        riotId: String,
        roleBoundItem: Int = 0,
        smite: Boolean = false,
        cs: Int = 200,
        neutralMinionsKilled: Int = 0,
        gold: Int = 12000,
        visionScore: Int = 25,
        wardsPlaced: Int = 8,
        damage: Int = 20000,
        totalDamageTaken: Int = 20000,
        damageDealtToObjectives: Int = 3000,
        lane: String? = null,
        role: String? = null,
    ) = MatchParticipant(
        riotId = riotId, champion = "Champ", team = "BLUE", teamId = 100, win = true,
        roleBoundItem = roleBoundItem,
        spell1Id = if (smite) SMITE else 4, spell2Id = 14,
        cs = cs, neutralMinionsKilled = neutralMinionsKilled, gold = gold,
        visionScore = visionScore, wardsPlaced = wardsPlaced, damage = damage,
        totalDamageTaken = totalDamageTaken, damageDealtToObjectives = damageDealtToObjectives,
        lane = lane, role = role,
    )

    @Test
    fun `역할 장화 넷이 있으면 나머지 한 명이 원딜로 확정된다`() {
        val team = listOf(
            p("top", roleBoundItem = TOP_BOOTS),
            p("jgl", roleBoundItem = JUNGLE_BOOTS, smite = true, neutralMinionsKilled = 150, cs = 170),
            p("mid", roleBoundItem = MID_BOOTS),
            p("sup", roleBoundItem = SUPPORT_BOOTS, cs = 30, gold = 8000, visionScore = 90),
            p("adc", roleBoundItem = PLAIN_BOOTS, cs = 260),
        )
        val r = PositionDetector.assignPositions(team)

        assertEquals(Position.TOP, r["top"])
        assertEquals(Position.JUNGLE, r["jgl"])
        assertEquals(Position.MID, r["mid"])
        assertEquals(Position.SUPPORT, r["sup"])
        assertEquals(Position.ADC, r["adc"])
    }

    @Test
    fun `역할 장화는 플레이 스탯이 정반대여도 이긴다`() {
        // 카이사 미드처럼 스탯만 보면 원딜로 보이는 픽. 챔피언도 딜 타입도 안 보고
        // 역할 장화만 보므로 미드로 남아야 한다. 이 판단이 뒤집히면 오프메타가 전부 깨진다.
        val team = listOf(
            p("top", roleBoundItem = TOP_BOOTS, totalDamageTaken = 40000),
            p("jgl", roleBoundItem = JUNGLE_BOOTS, smite = true, neutralMinionsKilled = 150, cs = 170),
            p("offmeta-mid", roleBoundItem = MID_BOOTS, cs = 300, gold = 18000, damage = 40000),
            p("sup", roleBoundItem = SUPPORT_BOOTS, cs = 20, gold = 7000, visionScore = 100),
            p("adc", roleBoundItem = PLAIN_BOOTS, cs = 210, gold = 11000, damage = 15000),
        )
        val r = PositionDetector.assignPositions(team)

        assertEquals(Position.MID, r["offmeta-mid"]) { "역할 장화가 스탯보다 우선해야 한다" }
        assertEquals(Position.ADC, r["adc"])
    }

    @Test
    fun `틀린 lane 태깅이 있어도 역할 장화가 이긴다`() {
        // 실데이터에서 lane=JUNGLE 태깅이 실제 정글의 1.5배였다. 가짜 정글 힌트가 진짜를 밀어냈다.
        val team = listOf(
            p("real-jgl", roleBoundItem = JUNGLE_BOOTS, smite = true, neutralMinionsKilled = 160, cs = 180),
            p("fake-jgl", roleBoundItem = TOP_BOOTS, lane = "JUNGLE", totalDamageTaken = 45000),
            p("mid", roleBoundItem = MID_BOOTS, lane = "JUNGLE"),
            p("sup", roleBoundItem = SUPPORT_BOOTS, cs = 25, gold = 7500, visionScore = 95),
            p("adc", roleBoundItem = PLAIN_BOOTS, cs = 250),
        )
        val r = PositionDetector.assignPositions(team)

        assertEquals(Position.JUNGLE, r["real-jgl"])
        assertEquals(Position.TOP, r["fake-jgl"])
        assertEquals(Position.MID, r["mid"])
    }

    @Test
    fun `역할 장화가 없으면 스마이트로 정글을 잡는다`() {
        // 옛 패치 데이터나 장화를 못 산 경기. 스마이트는 308팀 중 307팀에서 정확히 한 명이었다.
        val team = listOf(
            p("a", cs = 210, totalDamageTaken = 42000),
            p("jgl", smite = true, neutralMinionsKilled = 150, cs = 175, damageDealtToObjectives = 12000),
            p("c", cs = 230),
            p("sup", cs = 25, gold = 7000, visionScore = 95, wardsPlaced = 30),
            p("e", cs = 255, gold = 14000),
        )
        val r = PositionDetector.assignPositions(team)

        assertEquals(Position.JUNGLE, r["jgl"])
        assertEquals(Position.SUPPORT, r["sup"])
    }

    @Test
    fun `같은 역할 장화를 둘이 들면 못박지 않고 스탯으로 가른다`() {
        // 데이터가 깨진 경우의 안전장치. 실데이터 306팀에서는 한 건도 없었다.
        val team = listOf(
            p("x", roleBoundItem = MID_BOOTS),
            p("y", roleBoundItem = MID_BOOTS),
            p("jgl", roleBoundItem = JUNGLE_BOOTS, smite = true, neutralMinionsKilled = 150, cs = 170),
            p("sup", roleBoundItem = SUPPORT_BOOTS, cs = 25, gold = 7000, visionScore = 95),
            p("adc", roleBoundItem = PLAIN_BOOTS, cs = 250),
        )
        val r = PositionDetector.assignPositions(team)

        // 충돌한 둘은 강제되지 않지만, 나머지는 그대로 확정되고 5포지션이 하나씩 채워져야 한다
        assertEquals(Position.JUNGLE, r["jgl"])
        assertEquals(Position.SUPPORT, r["sup"])
        assertEquals(5, r.values.toSet().size) { "5개 포지션이 정확히 하나씩 배정돼야 한다: $r" }
    }

    @Test
    fun `항상 다섯 포지션이 하나씩 채워진다`() {
        val team = List(5) { p("p$it") }   // 아무 신호도 없는 팀
        val r = PositionDetector.assignPositions(team)
        assertEquals(5, r.size)
        assertEquals(
            setOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT),
            r.values.toSet(),
        )
    }
}
