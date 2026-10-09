package com.gijun.lolml.feature

import com.gijun.lolml.TEAM_A
import com.gijun.lolml.TEAM_B
import com.gijun.lolml.data.PlayerRank
import com.gijun.lolml.data.QueueRank
import com.gijun.lolml.match
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TierPriorTest {
    private fun rank(
        tier: String,
        division: String = "IV",
        lp: Int = 0,
    ) = QueueRank(tier, division, lp, wins = 10, losses = 10)

    private fun player(
        id: String,
        solo: QueueRank? = null,
        flex: QueueRank? = null,
    ) = id to PlayerRank(id, "$id#KR1", found = true, solo = solo, flex = flex, fetchedAt = 0L)

    @Test
    fun `한 티어에 1 이고 단계와 LP 는 그 사이를 나눈다`() {
        assertEquals(0.0, TierPrior.score(rank("IRON")), 1e-12)
        assertEquals(3.0, TierPrior.score(rank("GOLD")), 1e-12)
        assertEquals(4.625, TierPrior.score(rank("PLATINUM", "II", 50)), 1e-12)
        // 다이아 I 100LP 는 마스터 0LP 와 같은 자리다.
        assertEquals(TierPrior.score(rank("MASTER", "I", 0)), TierPrior.score(rank("DIAMOND", "I", 100)), 1e-12)
        assertEquals(8.0, TierPrior.score(rank("GRANDMASTER", "I", 400)), 1e-12)
    }

    @Test
    fun `솔로랭크가 먼저고 없으면 자유랭크, 둘 다 없으면 평균이다`() {
        val tiers =
            TierPrior(
                mapOf(
                    player("solo", solo = rank("GOLD"), flex = rank("DIAMOND")),
                    player("flex", flex = rank("EMERALD")),
                    player("none"),
                ),
            )

        // 점수는 골드 3, 에메랄드 5 → 평균 4
        assertEquals(-1.0, tiers.centered("solo"), 1e-12)
        assertEquals(1.0, tiers.centered("flex"), 1e-12)
        assertEquals(0.0, tiers.centered("none"), 1e-12)
        assertEquals(0.0, tiers.centered("스냅샷에 없는 사람"), 1e-12)
    }

    @Test
    fun `티어가 높은 팀은 첫 경기부터 앞선 것으로 본다`() {
        val ranks = (TEAM_A.map { player(it, solo = rank("PLATINUM")) } + TEAM_B.map { player(it, solo = rank("GOLD")) }).toMap()

        val first =
            FeatureBuilder(TierPrior(ranks))
                .build(listOf(match("m1", TEAM_A, TEAM_B, blueWin = true)))
                .single()
                .let { FeatureBuilder.NAMES.zip(it.features.toList()).toMap() }

        assertEquals(1.0, first.getValue("tierDiff"), 1e-12)
        assertEquals(FeatureBuilder.ELO_PER_TIER, first.getValue("tierLaneEloDiff"), 1e-9)
        // 내전 기록으로 만든 피처는 티어를 모른다.
        assertEquals(0.0, first.getValue("laneEloDiff"), 1e-12)
    }

    @Test
    fun `티어가 없으면 티어 라인 Elo 는 라인 Elo 와 똑같이 움직인다`() {
        val examples =
            FeatureBuilder().build(
                listOf(
                    match("m1", TEAM_A, TEAM_B, blueWin = true, blueGold = 12_000),
                    match("m2", TEAM_A, TEAM_B, blueWin = false, blueGold = 9_000),
                    match("m3", TEAM_A, TEAM_B, blueWin = true),
                ),
            )
        val lane = FeatureBuilder.NAMES.indexOf("laneEloDiff")
        val tierLane = FeatureBuilder.NAMES.indexOf("tierLaneEloDiff")

        assertContentEquals(examples.map { it.features[lane] }, examples.map { it.features[tierLane] })
    }
}
