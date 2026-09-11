package com.gijun.main.application.handler.query

import com.gijun.main.application.port.out.MatchPeriodSummary
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.PositionCount
import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 정글 지표.
 *
 * 카정 비율은 neutral_minions_killed_enemy_jungle 로 냈는데, 이 컬럼이 수집분
 * 1,716행 전부 0 이었다. 그래서 예전 코드에는 두 가지 문제가 있었다.
 *
 *   1) 장악 점수가 카정에 0.25 를 주고 있어 점수가 항상 4분의 3 스케일로 눌렸다.
 *   2) 태그 조건에 카정이 끼어 있어 "공격형"은 절대 안 나오고,
 *      "안전 갱킹형"은 뒤 조건이 항상 참이라 사실상 킬 관여만 보고 있었다.
 *
 * 여기서는 카정이 0 인 상태에서도 점수와 태그가 제대로 나오는지 고정한다.
 */
class GetJungleDominanceHandlerTest {

    /** 정글러로 잡히려면 정글 몹 30 이상이어야 한다. */
    private fun jungler(
        riotId: String,
        win: Boolean = true,
        kills: Int = 0,
        assists: Int = 0,
        jungleCs: Int = 150,
        objDamage: Int = 5_000,
        visionScore: Int = 20,
        teamId: Int = 100,
    ) = MatchParticipant(
        riotId = riotId, champion = "LeeSin", championId = 64, team = "BLUE", teamId = teamId, win = win,
        kills = kills, assists = assists,
        neutralMinionsKilled = jungleCs,
        // 실제 DB 와 같은 상태. 이 값이 0 이어도 지표가 망가지면 안 된다.
        neutralMinionsKilledEnemyJungle = 0,
        damageDealtToObjectives = objDamage,
        visionScore = visionScore,
    )

    /** 정글러가 아닌 팀원. 팀 합계(킬·오브젝트 딜)를 만들기 위한 들러리다. */
    private fun laner(riotId: String, kills: Int = 0, objDamage: Int = 0, teamId: Int = 100) =
        MatchParticipant(
            riotId = riotId, champion = "Ahri", championId = 103, team = "BLUE", teamId = teamId, win = true,
            kills = kills, neutralMinionsKilled = 0, damageDealtToObjectives = objDamage,
        )

    private fun handler(participants: List<MatchParticipant>, durationSec: Int = 1800): GetJungleDominanceHandler {
        val match = Match(
            matchId = "KR_1", queueId = 0, gameCreation = 1, gameDuration = durationSec,
            participants = participants.toMutableList(),
        )
        val port = object : MatchPersistencePort {
            override fun save(match: Match) = match
            override fun existsByMatchId(matchId: String) = false
            override fun findByMatchId(matchId: String): Match? = null
            override fun findAllWithParticipants(queueIds: List<Int>) = listOf(match)
            override fun findPageWithParticipants(queueIds: List<Int>, page: Int, size: Int) = listOf(match)
            override fun findPeriodSummary(queueIds: List<Int>) = MatchPeriodSummary(null, null, 0, 0)
            override fun deleteByMatchId(matchId: String) {}
            override fun countByQueueIds(queueIds: List<Int>) = 1L
            override fun findAllOrderedByGameCreation() = listOf(match)
            override fun updateAssignedPositions(updates: Map<Long, String>) {}
            override fun saveTimelineRaw(matchId: String, raw: String) {}
            override fun findTimelineRaw(matchIds: Collection<String>) = emptyMap<String, String>()
            override fun updateLaneMethods(updates: Map<String, LaneMethod>) {}
            override fun findPositionCounts() = emptyList<PositionCount>()
        }
        val cache = object : StatsCachePort {
            override fun <T> getOrCompute(key: String, compute: () -> T): T = compute()
            override fun evictAll() {}
            override fun evictByPrefix(prefix: String) {}
        }
        return GetJungleDominanceHandler(port, cache)
    }

    @Test
    fun `정글 몹이 적은 참가자는 정글러로 안 잡는다`() {
        val result = handler(listOf(
            jungler("정글#KR1", jungleCs = 150),
            laner("미드#KR1"),
        )).getJungleDominance("normal")

        assertEquals(listOf("정글#KR1"), result.rankings.map { it.riotId })
    }

    @Test
    fun `카정 데이터가 없어도 장악 점수가 0 으로 눌리지 않는다`() {
        // 오브젝트 딜을 팀에서 혼자 다 넣고 킬 관여도 100%, 시야도 넉넉한 정글러.
        // 카정 항이 남아 있었다면 만점의 4분의 3 언저리에서 천장이 막혔다.
        val result = handler(listOf(
            jungler("정글#KR1", kills = 10, assists = 0, objDamage = 10_000, visionScore = 30),
        )).getJungleDominance("normal")

        val entry = result.rankings.single()
        assertEquals(0.0, entry.avgInvadeRatio, "카정 컬럼이 비어 있으니 0 이어야 한다")
        // objShare 1.0*0.4 + kp 1.0*0.4 + visionPerMin 1.0*0.2 = 1.0
        assertEquals(1.0, entry.avgJungleDominance, 0.01)
    }

    @Test
    fun `오브젝트 딜 비중이 높으면 오브젝트 특화로 잡는다`() {
        val result = handler(listOf(
            jungler("정글#KR1", kills = 0, assists = 0, objDamage = 9_000),
            laner("미드#KR1", kills = 10, objDamage = 1_000),
        )).getJungleDominance("normal")

        assertEquals("오브젝트 특화", result.rankings.single().playStyleTag)
    }

    @Test
    fun `킬 관여가 높으면 갱킹형으로 잡는다`() {
        // 오브젝트 딜 비중은 낮게 두고 킬 관여만 높인다.
        val result = handler(listOf(
            jungler("정글#KR1", kills = 0, assists = 8, objDamage = 1_000),
            laner("미드#KR1", kills = 10, objDamage = 9_000),
        )).getJungleDominance("normal")

        val entry = result.rankings.single()
        assertTrue(entry.avgKp > 0.6, "킬 관여가 0.6 을 넘어야 하는 설정인데 ${entry.avgKp} 다")
        assertEquals("갱킹형", entry.playStyleTag)
    }

    @Test
    fun `정글 몹을 많이 먹고 관여가 적으면 파밍형으로 잡는다`() {
        // 예전에는 이 자리가 "공격형"이었는데 카정 조건 때문에 절대 안 나왔다.
        val result = handler(listOf(
            jungler("정글#KR1", kills = 0, assists = 0, jungleCs = 220, objDamage = 1_000),
            laner("미드#KR1", kills = 10, objDamage = 9_000),
        )).getJungleDominance("normal")

        assertEquals("파밍형", result.rankings.single().playStyleTag)
    }

    @Test
    fun `어디에도 안 걸리면 밸런스형이다`() {
        val result = handler(listOf(
            jungler("정글#KR1", kills = 1, assists = 0, jungleCs = 150, objDamage = 1_000),
            laner("미드#KR1", kills = 10, objDamage = 9_000),
        )).getJungleDominance("normal")

        assertEquals("밸런스형", result.rankings.single().playStyleTag)
    }

    @Test
    fun `장악 점수가 높은 순으로 세운다`() {
        val result = handler(listOf(
            jungler("약한정글#KR1", kills = 0, assists = 0, objDamage = 1_000, visionScore = 5, teamId = 100),
            laner("들러리#KR1", kills = 10, objDamage = 9_000, teamId = 100),
            jungler("센정글#KR2", kills = 10, assists = 0, objDamage = 9_000, visionScore = 40, teamId = 200),
        )).getJungleDominance("normal")

        assertEquals(listOf("센정글#KR2", "약한정글#KR1"), result.rankings.map { it.riotId })
    }
}
