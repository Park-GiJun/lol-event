package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.stats.result.EloLeaderboardResult
import com.gijun.main.application.dto.stats.result.EloRankEntry
import com.gijun.main.application.port.`in`.CalculateEloForMatchUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetEloUseCase
import com.gijun.main.application.port.`in`.ResetAndRecalculateEloUseCase
import com.gijun.main.application.port.out.EloPort
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.model.elo.PlayerElo
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.pow

@Service
class EloCalculationHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val eloPort: EloPort,
) : CalculateEloForMatchUseCase, ResetAndRecalculateEloUseCase, GetEloUseCase, GetEloLeaderboardUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val K = 24.0
        private const val ALPHA = 6.0
        private const val INITIAL_ELO = 1500.0
        private const val MIN_ELO = 100.0
    }

    // ────────── UseCase 구현 ──────────

    @Transactional
    override fun calculateForMatch(matchId: String) {
        val match = matchPersistencePort.findByMatchId(matchId)
            ?: run { log.warn("Elo 계산 skip — 매치 없음: $matchId"); return }
        processMatch(match)
    }

    @Transactional
    override fun resetAndRecalculate() {
        log.info("Elo 전체 초기화 시작")
        eloPort.deleteAll()
        val matches = matchPersistencePort.findAllOrderedByGameCreation()
        log.info("재집계 대상 매치: ${matches.size}개")
        matches.forEach { processMatch(it) }
        log.info("Elo 재집계 완료")
    }

    override fun getAll(): List<PlayerElo> = eloPort.findAll()
    override fun getByRiotId(riotId: String): PlayerElo? = eloPort.findByRiotId(riotId)

    override fun getLeaderboard(): EloLeaderboardResult {
        val sorted = eloPort.findAll().sortedByDescending { it.elo }
        return EloLeaderboardResult(
            players = sorted.mapIndexed { idx, elo ->
                EloRankEntry(rank = idx + 1, riotId = elo.riotId, elo = elo.elo, games = elo.games)
            }
        )
    }

    // ────────── 핵심 계산 ──────────

    private fun processMatch(match: Match) {
        val participants = match.participants
        if (participants.size < 2) return

        val riotIds = participants.mapNotNull { it.riotId.takeIf { r -> r.isNotBlank() } }
        val currentElos = eloPort.findAllByRiotIds(riotIds)
            .associate { it.riotId to it }
            .toMutableMap()

        val changes = calcEloChanges(match, currentElos.mapValues { it.value.elo })

        val updated = changes.map { (riotId, delta) ->
            val prev = currentElos[riotId]
            PlayerElo(
                id = prev?.id ?: 0,
                riotId = riotId,
                elo = maxOf(MIN_ELO, (prev?.elo ?: INITIAL_ELO) + delta),
                games = (prev?.games ?: 0) + 1,
                updatedAt = LocalDateTime.now(),
            )
        }
        eloPort.saveAll(updated)
        log.debug("Elo 업데이트 완료 — matchId=${match.matchId}, 대상=${updated.size}명")
    }

    private fun calcEloChanges(match: Match, eloMap: Map<String, Double>): Map<String, Double> {
        val teamA = match.participants.filter { it.teamId == 100 }
        val teamB = match.participants.filter { it.teamId == 200 }

        if (teamA.isEmpty() || teamB.isEmpty()) return emptyMap()

        val avgEloA = teamA.map { eloMap[it.riotId] ?: INITIAL_ELO }.average()
        val avgEloB = teamB.map { eloMap[it.riotId] ?: INITIAL_ELO }.average()

        val eA = 1.0 / (1 + 10.0.pow((avgEloB - avgEloA) / 400))
        val eB = 1.0 - eA

        val aWon = teamA.any { it.win }
        val deltaTeamA = K * ((if (aWon) 1.0 else 0.0) - eA)
        val deltaTeamB = K * ((if (!aWon) 1.0 else 0.0) - eB)

        val positionsA = assignPositions(teamA)
        val positionsB = assignPositions(teamB)

        val result = mutableMapOf<String, Double>()
        for (p in teamA) {
            val pos = positionsA[p.riotId] ?: Position.MID
            val perf = calcPerformanceScore(p, teamA, pos)
            result[p.riotId] = deltaTeamA + ALPHA * (perf - 0.5)
        }
        for (p in teamB) {
            val pos = positionsB[p.riotId] ?: Position.MID
            val perf = calcPerformanceScore(p, teamB, pos)
            result[p.riotId] = deltaTeamB + ALPHA * (perf - 0.5)
        }
        return result
    }

    // ────────── 포지션 배정 ──────────

    private enum class Position { TOP, JUNGLE, MID, ADC, SUPPORT }

    /**
     * 팀 5명에게 포지션을 1:1로 배정한다.
     * 1차: lane/role 명시값 사용
     * 2차: BOTTOM DUO 2명 → 딜+골드 높은 쪽 ADC, 낮은 쪽 SUPPORT
     * 3차: 나머지 미배정자 → 남은 포지션에 적합도 점수 기반 그리디 배정
     */
    private fun assignPositions(team: List<MatchParticipant>): Map<String, Position> {
        val assigned = mutableMapOf<String, Position>()
        val taken = mutableSetOf<Position>()
        val pending = mutableListOf<MatchParticipant>()

        // 1차: 명확한 lane/role 매핑
        for (p in team) {
            val pos = primaryPosition(p)
            if (pos != null && pos !in taken) {
                assigned[p.riotId] = pos
                taken.add(pos)
            } else {
                pending.add(p)
            }
        }

        // 2차: BOTTOM에 남아 있는 2명 → ADC / SUPPORT 분리
        val remaining = Position.entries.toMutableList().apply { removeAll(taken) }
        if (Position.ADC in remaining && Position.SUPPORT in remaining) {
            val bottomPending = pending.filter {
                it.lane == "BOTTOM" || it.role in listOf("DUO_CARRY", "DUO_SUPPORT", "DUO")
            }
            if (bottomPending.size == 2) {
                val adc = bottomPending.maxByOrNull { it.damage + it.gold }!!
                val sup = bottomPending.first { it.riotId != adc.riotId }
                assigned[adc.riotId] = Position.ADC
                assigned[sup.riotId] = Position.SUPPORT
                pending.removeAll(bottomPending.toSet())
                remaining.remove(Position.ADC)
                remaining.remove(Position.SUPPORT)
            } else if (bottomPending.size == 1) {
                // 한 명만 남은 경우 — 적합도로 결정
            }
        }

        // 3차: 나머지 → 남은 포지션에 적합도 그리디
        for (pos in remaining.toList()) {
            if (pending.isEmpty()) break
            val best = pending.maxByOrNull { positionScore(it, pos) }!!
            assigned[best.riotId] = pos
            pending.remove(best)
            remaining.remove(pos)
        }

        return assigned
    }

    private fun primaryPosition(p: MatchParticipant): Position? = when {
        p.lane == "TOP"                                    -> Position.TOP
        p.lane == "JUNGLE"                                 -> Position.JUNGLE
        p.lane == "MIDDLE"                                 -> Position.MID
        p.lane == "BOTTOM" && p.role == "CARRY"            -> Position.ADC
        p.lane == "BOTTOM" && p.role == "DUO_CARRY"        -> Position.ADC
        p.lane == "BOTTOM" && p.role == "SUPPORT"          -> Position.SUPPORT
        p.lane == "BOTTOM" && p.role == "DUO_SUPPORT"      -> Position.SUPPORT
        else                                               -> null
    }

    /** 포지션별 적합도 점수 — 미배정자를 남은 포지션에 배치할 때 사용 */
    private fun positionScore(p: MatchParticipant, pos: Position): Double = when (pos) {
        Position.TOP     -> p.cs * 0.4 + p.damage * 0.3 + p.totalDamageTaken * 0.3
        Position.JUNGLE  -> p.neutralMinionsKilled * 2.5 + p.damageDealtToObjectives * 0.5
        Position.MID     -> p.damage * 0.6 + p.cs * 0.4
        Position.ADC     -> p.damage * 0.5 + p.gold * 0.3 + p.cs * 0.2
        Position.SUPPORT -> p.visionScore * 3.0 + p.totalUnitsHealed * 0.5
    }

    // ────────── 퍼포먼스 점수 ──────────

    private fun calcPerformanceScore(
        p: MatchParticipant,
        team: List<MatchParticipant>,
        pos: Position,
    ): Double {
        val teamKills  = team.sumOf { it.kills }.coerceAtLeast(1)
        val teamDamage = team.sumOf { it.damage }.coerceAtLeast(1)
        val teamGold   = team.sumOf { it.gold }.coerceAtLeast(1)
        val teamVision = team.sumOf { it.visionScore }.coerceAtLeast(1)

        // KDA 팀 내 순위 점수 (0~1)
        val kda = (p.kills + p.assists).toDouble() / maxOf(1, p.deaths)
        val teamKdas = team.map { (it.kills + it.assists).toDouble() / maxOf(1, it.deaths) }
        val kdaMin = teamKdas.min()
        val kdaMax = teamKdas.max()
        val kdaRank = if (kdaMax == kdaMin) 0.5 else (kda - kdaMin) / (kdaMax - kdaMin)

        val kp          = (p.kills + p.assists).toDouble() / teamKills
        val dmgShare    = p.damage.toDouble() / teamDamage
        val goldShare   = p.gold.toDouble() / teamGold
        val visionShare = p.visionScore.toDouble() / teamVision

        return when (pos) {
            Position.TOP     -> 0.38 * kdaRank + 0.27 * kp + 0.20 * dmgShare + 0.15 * goldShare
            Position.JUNGLE  -> 0.33 * kdaRank + 0.30 * kp + 0.15 * dmgShare + 0.10 * goldShare + 0.12 * visionShare
            Position.MID     -> 0.34 * kdaRank + 0.24 * kp + 0.27 * dmgShare + 0.15 * goldShare
            Position.ADC     -> 0.25 * kdaRank + 0.20 * kp + 0.35 * dmgShare + 0.20 * goldShare
            Position.SUPPORT -> 0.18 * kdaRank + 0.22 * kp + 0.10 * dmgShare + 0.15 * goldShare + 0.35 * visionShare
        }
    }
}
