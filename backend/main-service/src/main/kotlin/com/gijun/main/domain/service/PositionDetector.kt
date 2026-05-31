package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position

/**
 * 한 팀(teamId 100/200)의 5명에게 TOP/JUNGLE/MID/ADC/SUPPORT 포지션을 1:1로 배정한다.
 *
 * ## 왜 새로 짰나
 * 과거 구현은 LCU 타임라인의 `lane`/`role` 값을 **그대로 신뢰**하고 greedy 로 배정했다.
 * 그런데 타임라인 lane/role 은 부정확하기로 악명 높아(탑이 JUNGLE 로, 정글러가 BOTTOM 으로 태깅 등)
 * 잘못된 1순위가 자리를 먼저 점유하면 나머지가 빈 슬롯으로 밀려나 "한 팀에 정글 둘 / ADC 없음" 같은
 * 깨진 배정이 나왔다.
 *
 * ## 새 접근
 * lane/role 은 **약한 힌트**로만 쓰고, 실제로 신뢰도 높은 플레이 신호를 종합해 점수화한다.
 *  - 스마이트 스펠(11): 정글의 거의 확정적 신호
 *  - 정글 몹 처치(neutralMinionsKilled): 정글
 *  - 라인 미니언 CS, 골드: ADC/미드/탑은 높고 서폿/정글은 낮음
 *  - 시야 점수·와드: 서포터
 *  - 받은 피해(totalDamageTaken): 탑(탱커/브루저)
 *  - 오브젝트 피해: 정글
 *
 * 그리고 팀이 정확히 5명이면 5! = 120 가지 배정을 **완전 탐색**해 총 적합도가 최대인
 * 1:1 순열을 고른다. 항상 5개 포지션이 정확히 하나씩 채워지는 것을 보장한다.
 */
object PositionDetector {

    private const val SMITE_SPELL_ID = 11

    private val LANE_POSITIONS = listOf(
        Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT
    )
    private val REQUIRED_POSITIONS = LANE_POSITIONS.toSet()

    /**
     * 팀의 assignedPosition 들이 이미 TOP/JUNGLE/MID/ADC/SUPPORT 를 정확히 하나씩
     * 가지고 있으면 true. (백필 시 "정상 팀 제외" 판단용)
     */
    fun isTeamPositioned(team: List<MatchParticipant>): Boolean {
        if (team.size != 5) return false
        val positions = team.mapNotNull { p ->
            p.assignedPosition.takeIf { it.isNotBlank() }?.let {
                runCatching { Position.valueOf(it.uppercase()) }.getOrNull()
            }
        }
        return positions.size == 5 && positions.toSet() == REQUIRED_POSITIONS
    }

    /**
     * 팀원에게 포지션을 배정한다. riotId -> Position.
     */
    fun assignPositions(team: List<MatchParticipant>): Map<String, Position> = when {
        team.isEmpty() -> emptyMap()
        team.size == 5 -> optimalAssignment(team)
        else           -> greedyAssignment(team)
    }

    /**
     * 매치 전체(양 팀)의 참가자에 assignedPosition 을 채워 새 리스트로 반환한다.
     */
    fun assignPositionsToAll(participants: List<MatchParticipant>): List<MatchParticipant> {
        val teamGroups = participants.groupBy { it.teamId }
        val positionsByTeam = teamGroups.mapValues { (_, team) -> assignPositions(team) }
        return participants.map { p ->
            val pos = positionsByTeam[p.teamId]?.get(p.riotId) ?: Position.UNKNOWN
            p.copy(assignedPosition = pos.name)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  5명 → 5! 완전 탐색으로 총 적합도 최대 순열 선택
    // ─────────────────────────────────────────────────────────────

    private fun optimalAssignment(team: List<MatchParticipant>): Map<String, Position> {
        val feats = teamFeatures(team)
        // fit[i][j] = i번째 플레이어가 j번째 포지션(LANE_POSITIONS)에 맞는 정도
        val fit = Array(5) { i -> DoubleArray(5) { j -> fitScore(team[i], feats[i], LANE_POSITIONS[j]) } }

        var bestScore = Double.NEGATIVE_INFINITY
        var bestPerm: IntArray? = null
        permutations(intArrayOf(0, 1, 2, 3, 4)) { perm ->
            var sum = 0.0
            for (i in 0 until 5) sum += fit[i][perm[i]]
            if (sum > bestScore) {
                bestScore = sum
                bestPerm = perm.copyOf()
            }
        }

        val result = mutableMapOf<String, Position>()
        val perm = bestPerm!!
        for (i in 0 until 5) result[team[i].riotId] = LANE_POSITIONS[perm[i]]
        return result
    }

    /** 표준 5인이 아닌 팀(예: 비정상 데이터) — 가장 점수 높은 (플레이어,포지션) 쌍부터 greedy 로 1:1 배정. */
    private fun greedyAssignment(team: List<MatchParticipant>): Map<String, Position> {
        val feats = teamFeatures(team)
        data class Cell(val idx: Int, val pos: Position, val score: Double)
        val cells = team.indices.flatMap { i ->
            LANE_POSITIONS.map { pos -> Cell(i, pos, fitScore(team[i], feats[i], pos)) }
        }.sortedByDescending { it.score }

        val assigned = mutableMapOf<String, Position>()
        val takenPlayers = mutableSetOf<Int>()
        val takenPositions = mutableSetOf<Position>()
        for (c in cells) {
            if (c.idx in takenPlayers || c.pos in takenPositions) continue
            assigned[team[c.idx].riotId] = c.pos
            takenPlayers.add(c.idx); takenPositions.add(c.pos)
            if (takenPlayers.size == team.size) break
        }
        // 포지션이 다 떨어졌는데 남은 플레이어 → UNKNOWN
        for (p in team) assigned.putIfAbsent(p.riotId, Position.UNKNOWN)
        return assigned
    }

    // ─────────────────────────────────────────────────────────────
    //  적합도 점수
    // ─────────────────────────────────────────────────────────────

    /** 팀 내 각 플레이어의 정규화(0~1) 특징값. fitScore 가 팀 상대 비교를 하도록 미리 계산. */
    private class Feat(
        val smite: Boolean,
        val nJungleCs: Double,
        val nLaneCs: Double,
        val nGold: Double,
        val nVision: Double,
        val nWards: Double,
        val nDamage: Double,
        val nTank: Double,
        val nObjDmg: Double,
    )

    private fun teamFeatures(team: List<MatchParticipant>): List<Feat> {
        fun laneCs(p: MatchParticipant) = (p.cs - p.neutralMinionsKilled).coerceAtLeast(0)

        val jungleCs = team.map { it.neutralMinionsKilled.toDouble() }
        val laneCsList = team.map { laneCs(it).toDouble() }
        val gold = team.map { it.gold.toDouble() }
        val vision = team.map { it.visionScore.toDouble() }
        val wards = team.map { (it.wardsPlaced + it.wardsKilled).toDouble() }
        val damage = team.map { it.damage.toDouble() }
        val tank = team.map { it.totalDamageTaken.toDouble() }
        val objDmg = team.map { it.damageDealtToObjectives.toDouble() }

        return team.indices.map { i ->
            val p = team[i]
            Feat(
                smite = p.spell1Id == SMITE_SPELL_ID || p.spell2Id == SMITE_SPELL_ID,
                nJungleCs = normalize(jungleCs[i], jungleCs),
                nLaneCs = normalize(laneCsList[i], laneCsList),
                nGold = normalize(gold[i], gold),
                nVision = normalize(vision[i], vision),
                nWards = normalize(wards[i], wards),
                nDamage = normalize(damage[i], damage),
                nTank = normalize(tank[i], tank),
                nObjDmg = normalize(objDmg[i], objDmg),
            )
        }
    }

    /**
     * 플레이어가 특정 포지션에 얼마나 맞는지 점수화.
     * lane/role 힌트는 +1.0 의 약한 가중치 — 강한 플레이 신호(스마이트=+3.0)는 잘못된 힌트를 덮어쓴다.
     */
    private fun fitScore(p: MatchParticipant, f: Feat, pos: Position): Double {
        val smiteBonus = if (f.smite) 1.0 else 0.0
        val laneHint = if (primaryPosition(p) == pos) 1.0 else 0.0

        val base = when (pos) {
            Position.JUNGLE ->
                3.0 * smiteBonus + 2.5 * f.nJungleCs + 0.8 * f.nObjDmg - 1.0 * f.nLaneCs
            Position.SUPPORT ->
                1.5 * f.nVision + 1.0 * f.nWards + 1.2 * (1 - f.nGold) + 1.0 * (1 - f.nLaneCs) - 2.0 * smiteBonus
            Position.ADC ->
                1.2 * f.nLaneCs + 1.2 * f.nDamage + 0.8 * f.nGold - 0.5 * f.nVision - 2.0 * smiteBonus
            Position.MID ->
                1.2 * f.nDamage + 0.6 * f.nLaneCs - 0.8 * f.nJungleCs - 0.3 * f.nVision - 1.5 * smiteBonus
            Position.TOP ->
                1.2 * f.nTank + 0.7 * f.nLaneCs - 0.4 * f.nVision - 0.8 * f.nJungleCs - 1.5 * smiteBonus
            Position.UNKNOWN -> 0.0
        }
        return base + laneHint
    }

    /** lane/role 기반 약한 힌트 (배정의 1순위 강제값이 아니라 동점 보정용). */
    fun primaryPosition(p: MatchParticipant): Position? = when {
        p.lane == "TOP"                                 -> Position.TOP
        p.lane == "JUNGLE"                              -> Position.JUNGLE
        p.lane == "MIDDLE"                              -> Position.MID
        p.lane == "BOTTOM" && p.role == "CARRY"         -> Position.ADC
        p.lane == "BOTTOM" && p.role == "DUO_CARRY"     -> Position.ADC
        p.lane == "BOTTOM" && p.role == "SUPPORT"       -> Position.SUPPORT
        p.lane == "BOTTOM" && p.role == "DUO_SUPPORT"   -> Position.SUPPORT
        else                                            -> null
    }

    // ─────────────────────────────────────────────────────────────
    //  유틸리티
    // ─────────────────────────────────────────────────────────────

    private fun normalize(value: Double, all: List<Double>): Double {
        val min = all.min()
        val max = all.max()
        return if (max == min) 0.5 else (value - min) / (max - min)
    }

    /** Heap's algorithm — arr 의 모든 순열을 visit 으로 콜백. */
    private inline fun permutations(arr: IntArray, visit: (IntArray) -> Unit) {
        val n = arr.size
        val c = IntArray(n)
        visit(arr)
        var i = 0
        while (i < n) {
            if (c[i] < i) {
                val swapIdx = if (i % 2 == 0) 0 else c[i]
                val tmp = arr[swapIdx]; arr[swapIdx] = arr[i]; arr[i] = tmp
                visit(arr)
                c[i]++
                i = 0
            } else {
                c[i] = 0
                i++
            }
        }
    }
}
