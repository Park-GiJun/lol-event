package com.gijun.main.domain.team.service

import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.team.exception.InvalidTeamBuildRequestException
import com.gijun.main.domain.team.model.TeamBuildModel
import com.gijun.main.domain.team.model.TeamCandidateModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TeamBalancerTest {
    private val all = TeamBalancer.LANES.toSet()

    @Test
    fun `5 명씩 나누고 한 사람도 빠뜨리거나 겹치지 않는다`() {
        listOf(10, 15, 20, 25, 30).forEach { size ->
            val players = flexible(size)

            val result = TeamBalancer.build(players)

            assertEquals(size / 5, result.teams.size)
            assertEquals(players.map { it.riotId }.sorted(), result.riotIds().sorted())
            result.teams.forEach { team -> assertEquals(TeamBalancer.LANES, team.slots.map { it.position }) }
        }
    }

    @Test
    fun `팀 평균 Elo 를 맞춘다`() {
        // 센 사람 다섯과 약한 사람 다섯. 그냥 앞에서부터 자르면 평균이 400 차이 난다.
        val players = (1..5).map { player("강$it", 1700.0) } + (1..5).map { player("약$it", 1300.0) }

        val result = TeamBalancer.build(players)

        assertTrue(result.eloSpread <= 80.0) { "평균 차가 너무 크다: ${result.eloSpread}" }
    }

    @Test
    fun `묶음은 같은 팀에 둔다`() {
        val players = flexible(15)
        val groups = listOf(listOf("p1", "p2", "p3"), listOf("p7", "p12"))

        val result = TeamBalancer.build(players, groups)

        groups.forEach { group ->
            val teams = group.map { member -> result.teams.indexOfFirst { team -> team.slots.any { it.riotId == member } } }
            assertEquals(1, teams.distinct().size) { "$group 가 갈라졌다: $teams" }
        }
    }

    @Test
    fun `묶음을 지키느라 평균이 벌어져도 묶음이 먼저다`() {
        val players = (1..5).map { player("강$it", 1700.0) } + (1..5).map { player("약$it", 1300.0) }

        val result = TeamBalancer.build(players, listOf((1..5).map { "강$it" }))

        assertEquals(400.0, result.eloSpread, 0.001)
    }

    @Test
    fun `갈 수 있는 포지션에만 앉힌다`() {
        // 아랑택처럼 서포터만 빼고 다 가는 사람과, 한 자리만 가는 사람들을 섞는다.
        val players =
            listOf(
                player("아랑택", 1400.0, all - Position.SUPPORT),
                player("탑1", 1500.0, setOf(Position.TOP)),
                player("정글1", 1500.0, setOf(Position.JUNGLE)),
                player("미드1", 1500.0, setOf(Position.MID)),
                player("원딜1", 1500.0, setOf(Position.ADC)),
                player("서폿1", 1500.0, setOf(Position.SUPPORT)),
                player("서폿2", 1500.0, setOf(Position.SUPPORT)),
                player("만능1", 1500.0, all),
                player("만능2", 1500.0, all),
                player("만능3", 1500.0, all),
            )

        val result = TeamBalancer.build(players)

        assertFalse(result.positionConflict)
        result.teams.flatMap { it.slots }.forEach { slot ->
            val allowed = players.first { it.riotId == slot.riotId }.positions
            assertTrue(slot.position in allowed) { "${slot.riotId} 가 ${slot.position} 에 앉았다" }
        }
    }

    @Test
    fun `여러 자리가 되면 많이 해 본 자리에 앉힌다`() {
        val veteran = player("탑장인", 1500.0, all, mapOf(Position.TOP to 50, Position.MID to 2))
        val players = listOf(veteran) + (1..9).map { player("만능$it", 1500.0, all) }

        val result = TeamBalancer.build(players)

        assertEquals(
            Position.TOP,
            result.teams
                .flatMap { it.slots }
                .first { it.riotId == "탑장인" }
                .position,
        )
    }

    @Test
    fun `자리를 다 채울 수 없으면 포기하지 않고 알린다`() {
        // 열 명 전원이 미드만 간다.
        val players = (1..10).map { player("미드$it", 1500.0, setOf(Position.MID)) }

        val result = TeamBalancer.build(players)

        assertTrue(result.positionConflict)
        assertEquals(8, result.teams.flatMap { it.slots }.count { it.offRole })
    }

    @Test
    fun `같은 seed 는 같은 편성을 준다`() {
        val players = flexible(20)

        assertEquals(TeamBalancer.build(players, seed = 7), TeamBalancer.build(players, seed = 7))
    }

    @Test
    fun `잘못된 요청은 무엇이 틀렸는지 말하며 거절한다`() {
        val players = flexible(10)

        assertInvalid { TeamBalancer.build(flexible(12)) }
        assertInvalid { TeamBalancer.build(flexible(5)) }
        assertInvalid { TeamBalancer.build(players + players.first()) }
        assertInvalid { TeamBalancer.build(players, listOf(listOf("p1", "없는사람"))) }
        assertInvalid { TeamBalancer.build(players, listOf(listOf("p1", "p2"), listOf("p2", "p3"))) }
        assertInvalid { TeamBalancer.build(players, listOf((1..6).map { "p$it" })) }
        assertInvalid { TeamBalancer.build(players.drop(1) + player("p1", 1500.0, emptySet())) }
        // 4·3·3 은 합이 10 이어도 5 명씩 담을 수 없다.
        assertInvalid {
            TeamBalancer.build(players, listOf(listOf("p1", "p2", "p3", "p4"), listOf("p5", "p6", "p7"), listOf("p8", "p9", "p10")))
        }
    }

    private fun assertInvalid(block: () -> Unit) {
        assertThrows(InvalidTeamBuildRequestException::class.java) { block() }
    }

    private fun TeamBuildModel.riotIds() = teams.flatMap { team -> team.slots.map { it.riotId } }

    private fun flexible(size: Int) = (1..size).map { player("p$it", 1300.0 + it * 17) }

    private fun player(
        riotId: String,
        elo: Double,
        positions: Set<Position> = all,
        games: Map<Position, Int> = emptyMap(),
    ) = TeamCandidateModel(riotId, elo, positions, games)
}
