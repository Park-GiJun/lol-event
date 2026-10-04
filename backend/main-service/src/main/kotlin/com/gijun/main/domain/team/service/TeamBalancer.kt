package com.gijun.main.domain.team.service

import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.team.exception.InvalidTeamBuildRequestException
import com.gijun.main.domain.team.model.BuiltTeamModel
import com.gijun.main.domain.team.model.TeamBuildModel
import com.gijun.main.domain.team.model.TeamCandidateModel
import com.gijun.main.domain.team.model.TeamSlotModel
import kotlin.random.Random

/**
 * 사람들을 5 명씩 나눠 **팀 평균 자리 Elo 가 비슷해지게** 편성한다.
 *
 * 지키는 조건은 둘이다.
 *  1. 같은 팀이어야 하는 묶음은 갈라놓지 않는다.
 *  2. 각자 갈 수 있는 포지션 안에서 다섯 자리(탑·정글·미드·원딜·서포터)를 채운다.
 *
 * 2 번을 도저히 맞출 수 없으면(예: 열 명 중 서포터 가능한 사람이 한 명) 편성을 포기하지 않고,
 * 누군가를 못 가는 자리에 앉힌 뒤 `positionConflict` 로 알린다. 막는 것보다 보여 주는 편이 쓸모 있다.
 *
 * ### 왜 평균 Elo 인가
 * 이 저장소의 검증(`RatingValidationResult`)에서 살아남은 예측기는 라인 Elo 팀 평균 하나였다.
 * 개인 지표나 듀오 시너지를 섞은 모델은 전부 기준선보다 나빴다. 그래서 여기서도 그것만 맞춘다.
 *
 * ### 왜 사람의 Elo 가 아니라 자리의 Elo 인가
 * 라인 Elo 는 사람당 값 하나다. 그대로 쓰면 탑이 주 포지션인 사람을 원딜에 앉혀도 탑 실력으로 계산된다.
 * 그래서 팀 강도는 **앉힌 자리에서의 라인 Elo**([TeamCandidateModel.eloAt])로 잰다. 그 값은 같은 라인
 * 맞대결 결과에서 나오므로(`SeatRatings`) 위 원칙 — 개인 지표를 섞지 않는다 — 은 그대로다.
 *
 * ### 어떻게 찾는가
 * 30 명을 6 팀으로 나누는 경우의 수는 전수 탐색이 안 된다. 무작위로 나눈 뒤 두 팀 사이에서 사람을
 * 맞바꿔 가며 좋아지는 쪽으로만 가고(언덕 오르기), 이걸 여러 번 다시 시작해 그중 좋은 것을 고른다.
 */
object TeamBalancer {
    const val TEAM_SIZE = 5

    /** 편성에 쓰는 다섯 자리. [Position.UNKNOWN] 은 자리가 아니다. */
    val LANES: List<Position> = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

    /** 못 가는 자리에 앉은 사람은 이만큼 약하다고 본다. 편성이 그런 자리를 피하게 만드는 값이다. */
    private const val OFF_ROLE_ELO_PENALTY = 150.0

    /** 못 가는 자리 하나가 Elo 차 몇 점만큼 나쁜가. 조건을 지키는 편성이 항상 이기게 크게 둔다. */
    private const val OFF_ROLE_COST = 10_000.0

    /** 같은 라인끼리의 Elo 차도 조금 본다. 팀 평균이 같아도 한 라인이 일방적이면 재미가 없다. */
    private const val LANE_SPREAD_WEIGHT = 0.1

    private const val RESTARTS = 200

    /** 최선과 이 점수 안쪽이면 "비슷하게 좋은 편성" 으로 본다. 다시 짜기는 그 안에서 고른다. */
    private const val NEAR_BEST_TOLERANCE = 6.0

    private val LANE_ORDERS: List<List<Position>> = permutations(LANES)

    /**
     * @param togetherGroups 같은 팀이어야 하는 사람들의 묶음.
     * @param seed 같은 값이면 같은 편성이 나온다. 바꾸면 비슷하게 좋은 다른 편성을 준다("다시 짜기").
     */
    fun build(
        candidates: List<TeamCandidateModel>,
        togetherGroups: List<List<String>> = emptyList(),
        seed: Long = 0,
    ): TeamBuildModel {
        validate(candidates, togetherGroups)

        val byId = candidates.associateBy { it.riotId }
        val teamCount = candidates.size / TEAM_SIZE
        val units = unitsOf(candidates, togetherGroups)
        val random = Random(seed)
        val evaluator = Evaluator(byId)

        val found = LinkedHashMap<List<List<String>>, Double>()
        repeat(RESTARTS) {
            val start = pack(units, teamCount, random) ?: throw InvalidTeamBuildRequestException(PACKING_FAILED)
            val climbed = climb(start, evaluator)
            found.putIfAbsent(canonical(climbed), evaluator.cost(climbed))
        }

        val best = found.values.min()
        val nearBest = found.filterValues { it <= best + NEAR_BEST_TOLERANCE }.keys.toList()
        val chosen = nearBest[random.nextInt(nearBest.size)]

        val teams = chosen.map { evaluator.seat(it) }.sortedByDescending { it.averageElo }
        return TeamBuildModel(
            teams = teams,
            eloSpread = teams.maxOf { it.averageElo } - teams.minOf { it.averageElo },
            positionConflict = teams.any { team -> team.slots.any { it.offRole } },
        )
    }

    private fun validate(
        candidates: List<TeamCandidateModel>,
        togetherGroups: List<List<String>>,
    ) {
        val ids = candidates.map { it.riotId }
        if (ids.isEmpty() || ids.size % TEAM_SIZE != 0 || ids.size < TEAM_SIZE * 2) {
            throw InvalidTeamBuildRequestException("인원은 10 명 이상이고 5 의 배수여야 합니다 (지금 ${ids.size} 명)")
        }
        val duplicated =
            ids
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        if (duplicated.isNotEmpty()) throw InvalidTeamBuildRequestException("같은 사람이 두 번 들어 있습니다: $duplicated")

        candidates.firstOrNull { it.positions.isEmpty() || Position.UNKNOWN in it.positions }?.let {
            throw InvalidTeamBuildRequestException("${it.riotId} 의 가능 포지션이 비어 있거나 올바르지 않습니다")
        }

        val grouped = togetherGroups.flatten()
        val unknown = grouped.filterNot { it in ids }
        if (unknown.isNotEmpty()) throw InvalidTeamBuildRequestException("묶음에 편성 대상이 아닌 사람이 있습니다: $unknown")
        val inTwoGroups =
            grouped
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        if (inTwoGroups.isNotEmpty()) throw InvalidTeamBuildRequestException("한 사람이 두 묶음에 들어 있습니다: $inTwoGroups")
        togetherGroups.firstOrNull { it.size > TEAM_SIZE }?.let {
            throw InvalidTeamBuildRequestException("한 묶음은 $TEAM_SIZE 명까지입니다: $it")
        }
    }

    /** 묶음은 한 덩어리로, 나머지는 한 명짜리 덩어리로. */
    private fun unitsOf(
        candidates: List<TeamCandidateModel>,
        togetherGroups: List<List<String>>,
    ): List<List<String>> {
        val groups = togetherGroups.filter { it.size > 1 }
        val grouped = groups.flatten().toSet()
        return groups + candidates.map { it.riotId }.filterNot { it in grouped }.map { listOf(it) }
    }

    /**
     * 덩어리들을 팀에 무작위로 담는다. 큰 덩어리부터 넣고, 막히면 되돌아가 다른 팀을 시도한다.
     *
     * @return 묶음 크기 때문에 어떻게 해도 5 명씩 나눌 수 없으면 null (예: 10 명에 4·3·3 묶음).
     */
    private fun pack(
        units: List<List<String>>,
        teamCount: Int,
        random: Random,
    ): List<List<List<String>>>? {
        val ordered = units.shuffled(random).sortedByDescending { it.size }
        val teams = List(teamCount) { mutableListOf<List<String>>() }
        val sizes = IntArray(teamCount)

        fun place(index: Int): Boolean {
            if (index == ordered.size) return true
            val unit = ordered[index]
            for (team in (0 until teamCount).shuffled(random)) {
                if (sizes[team] + unit.size > TEAM_SIZE) continue
                teams[team] += unit
                sizes[team] += unit.size
                if (place(index + 1)) return true
                teams[team].removeLast()
                sizes[team] -= unit.size
            }
            return false
        }
        return if (place(0)) teams.map { it.toList() } else null
    }

    /** 두 팀 사이에서 크기가 같은 덩어리를 맞바꿔, 좋아지는 동안 계속한다. */
    private fun climb(
        start: List<List<List<String>>>,
        evaluator: Evaluator,
    ): List<List<List<String>>> {
        val teams = start.map { it.toMutableList() }
        var cost = evaluator.cost(teams)
        var improved = true
        while (improved) {
            improved = false
            for (a in teams.indices) {
                for (b in a + 1 until teams.size) {
                    for (i in teams[a].indices) {
                        for (j in teams[b].indices) {
                            if (teams[a][i].size != teams[b][j].size) continue
                            swap(teams, a, i, b, j)
                            val next = evaluator.cost(teams)
                            if (next < cost - EPSILON) {
                                cost = next
                                improved = true
                            } else {
                                swap(teams, a, i, b, j)
                            }
                        }
                    }
                }
            }
        }
        return teams
    }

    private fun swap(
        teams: List<MutableList<List<String>>>,
        a: Int,
        i: Int,
        b: Int,
        j: Int,
    ) {
        val moved = teams[a][i]
        teams[a][i] = teams[b][j]
        teams[b][j] = moved
    }

    /** 팀 순서·팀 안 순서와 무관하게 같은 편성은 같은 값이 되게 한다. */
    private fun canonical(teams: List<List<List<String>>>): List<List<String>> = teams.map { it.flatten().sorted() }.sortedBy { it.first() }

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.size <= 1) {
            listOf(items)
        } else {
            items.flatMap { head -> permutations(items - head).map { listOf(head) + it } }
        }

    private const val EPSILON = 1e-9
    private const val PACKING_FAILED = "묶음의 크기 때문에 5 명씩 나눌 수 없습니다. 묶음을 줄이거나 나눠 주세요."

    /** 한 팀을 가장 잘 앉힌 결과. */
    private class Seating(
        val order: List<Position>,
        val offRoles: Int,
        val familiarity: Double,
        val laneElo: DoubleArray,
    ) {
        val averageElo: Double = laneElo.average()
    }

    private class Evaluator(
        private val byId: Map<String, TeamCandidateModel>,
    ) {
        // 맞바꾸기 한 번에 두 팀만 바뀐다. 나머지 팀은 전에 계산한 것을 다시 쓴다.
        private val cache = HashMap<List<String>, Seating>()

        fun cost(teams: List<List<List<String>>>): Double {
            val seatings = teams.map { seating(it.flatten()) }
            val averages = seatings.map { it.averageElo }
            val teamSpread = averages.max() - averages.min()
            val laneSpread =
                LANES.indices.sumOf { lane -> seatings.maxOf { it.laneElo[lane] } - seatings.minOf { it.laneElo[lane] } } / LANES.size
            return seatings.sumOf { it.offRoles } * OFF_ROLE_COST + teamSpread + laneSpread * LANE_SPREAD_WEIGHT
        }

        fun seat(members: List<String>): BuiltTeamModel {
            val seating = seating(members)
            val sorted = members.sorted()
            val slots =
                LANES.map { lane ->
                    val member = byId.getValue(sorted[seating.order.indexOf(lane)])
                    TeamSlotModel(member.riotId, lane, member.eloAt(lane), offRole = lane !in member.positions)
                }
            return BuiltTeamModel(slots, slots.map { it.elo }.average())
        }

        /** 다섯 명을 다섯 자리에 앉히는 120 가지 중, 못 가는 자리가 가장 적고 익숙한 자리가 많은 것. */
        private fun seating(members: List<String>): Seating {
            val sorted = members.sorted()
            return cache.getOrPut(sorted) {
                val players = sorted.map { byId.getValue(it) }
                LANE_ORDERS
                    .map { order -> seatingOf(players, order) }
                    .maxWith(compareBy<Seating> { -it.offRoles }.thenBy { it.familiarity })
            }
        }

        /** @param order `order[i]` 가 `players[i]` 의 자리. */
        private fun seatingOf(
            players: List<TeamCandidateModel>,
            order: List<Position>,
        ): Seating {
            var offRoles = 0
            var familiarity = 0.0
            val laneElo = DoubleArray(LANES.size)
            players.forEachIndexed { index, player ->
                val lane = order[index]
                val off = lane !in player.positions
                if (off) offRoles++
                val total = player.positionGames.values.sum()
                if (total > 0) familiarity += (player.positionGames[lane] ?: 0).toDouble() / total
                laneElo[LANES.indexOf(lane)] = player.eloAt(lane) - if (off) OFF_ROLE_ELO_PENALTY else 0.0
            }
            return Seating(order, offRoles, familiarity, laneElo)
        }
    }
}
