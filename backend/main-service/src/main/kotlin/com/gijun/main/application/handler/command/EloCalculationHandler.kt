package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.stats.result.EloHistoryEntry
import com.gijun.main.application.dto.stats.result.EloLeaderboardResult
import com.gijun.main.application.dto.stats.result.EloRankEntry
import com.gijun.main.application.dto.stats.result.PlayerEloHistoryResult
import com.gijun.main.application.port.`in`.CalculateEloForMatchUseCase
import com.gijun.main.application.port.`in`.GetEloHistoryUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetEloUseCase
import com.gijun.main.application.port.`in`.ResetAndRecalculateEloUseCase
import com.gijun.main.application.port.out.EloHistoryPort
import com.gijun.main.application.port.out.EloPort
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.model.elo.PlayerElo
import com.gijun.main.domain.model.elo.PlayerEloHistory
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
    private val eloHistoryPort: EloHistoryPort,
) : CalculateEloForMatchUseCase, ResetAndRecalculateEloUseCase, GetEloUseCase, GetEloLeaderboardUseCase, GetEloHistoryUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        /**
         * 퍼포먼스 보정폭 — 팀 내 최상/최하 대비 ±ALPHA/2 변동
         * 기본 K=28 기준 최대 총 변동: 28 + 6 = 34 (압도적 승), -28 - 6 = -34 (압도적 패)
         */
        private const val ALPHA       = 12.0
        private const val INITIAL_ELO = 1000.0
        private const val MIN_ELO     = 100.0

        /**
         * 동적 K-팩터: 게임 수에 따라 변동폭 조정
         *   0~9  판  → 64 (배치 기간: 초기 순위 빠르게 수렴)
         *  10~24 판  → 48 (성장 기간)
         *  25~49 판  → 36 (안정화)
         *  50+   판  → 28 (정착: 베테랑 선수 등급 보호)
         */
        fun kFactor(games: Int): Double = when {
            games < 10  -> 64.0
            games < 25  -> 48.0
            games < 50  -> 36.0
            else        -> 28.0
        }

        /**
         * 연승/연패 배율 — 핫/콜드 스트릭 반영
         *
         * 연승:
         *   3~4연승 → ×1.07  (이기면 조금 더 오르고, 지면 조금 더 내려감)
         *   5+ 연승 → ×1.15
         * 연패:
         *   3~4연패 → ×1.06  (빠른 하락으로 실력대 빠르게 재배치)
         *   5+ 연패 → ×1.12
         */
        fun streakMultiplier(winStreak: Int, lossStreak: Int, won: Boolean): Double =
            if (won) when {
                winStreak  >= 5 -> 1.15
                winStreak  >= 3 -> 1.07
                else            -> 1.0
            } else when {
                lossStreak >= 5 -> 1.12
                lossStreak >= 3 -> 1.06
                else            -> 1.0
            }
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
        eloHistoryPort.deleteAll()
        val matches = matchPersistencePort.findAllOrderedByGameCreation()
        log.info("재집계 대상 매치: ${matches.size}개")
        matches.forEach { processMatch(it) }
        log.info("Elo 재집계 완료")
    }

    override fun getAll(): List<PlayerElo> = eloPort.findAll()
    override fun getByRiotId(riotId: String): PlayerElo? = eloPort.findByRiotId(riotId)

    override fun getHistory(riotId: String, limit: Int): PlayerEloHistoryResult {
        val allElos = eloPort.findAll().sortedByDescending { it.elo }
        val myElo   = allElos.firstOrNull { it.riotId == riotId }
        val currentElo = myElo?.elo ?: INITIAL_ELO
        val eloRank    = allElos.indexOfFirst { it.riotId == riotId }.takeIf { it >= 0 }?.plus(1)
        val history    = eloHistoryPort.findByRiotId(riotId, limit).map {
            EloHistoryEntry(
                matchId = it.matchId, eloBefore = it.eloBefore, eloAfter = it.eloAfter,
                delta = it.delta, win = it.win, gameCreation = it.gameCreation,
            )
        }
        return PlayerEloHistoryResult(riotId = riotId, currentElo = currentElo, eloRank = eloRank, history = history)
    }

    override fun getLeaderboard(): EloLeaderboardResult {
        val sorted = eloPort.findAll().sortedByDescending { it.elo }
        return EloLeaderboardResult(
            players = sorted.mapIndexed { idx, elo ->
                val wr = if (elo.games > 0) elo.wins * 100.0 / elo.games else 0.0
                EloRankEntry(
                    rank       = idx + 1,
                    riotId     = elo.riotId,
                    elo        = elo.elo,
                    games      = elo.games,
                    wins       = elo.wins,
                    losses     = elo.losses,
                    winRate    = (wr * 10).toLong() / 10.0,
                    winStreak  = elo.winStreak,
                    lossStreak = elo.lossStreak,
                )
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

        val changes = calcEloChanges(match, currentElos)

        val now = LocalDateTime.now()
        val updated = changes.map { (riotId, delta) ->
            val prev        = currentElos[riotId]
            val participant = match.participants.firstOrNull { it.riotId == riotId }
            val won         = participant?.win ?: false

            val newElo        = maxOf(MIN_ELO, (prev?.elo ?: INITIAL_ELO) + delta)
            val newGames      = (prev?.games ?: 0) + 1
            val newWins       = (prev?.wins ?: 0) + (if (won) 1 else 0)
            val newLosses     = (prev?.losses ?: 0) + (if (!won) 1 else 0)
            val newWinStreak  = if (won) (prev?.winStreak ?: 0) + 1 else 0
            val newLossStreak = if (!won) (prev?.lossStreak ?: 0) + 1 else 0

            PlayerElo(
                id         = prev?.id ?: 0,
                riotId     = riotId,
                elo        = newElo,
                games      = newGames,
                wins       = newWins,
                losses     = newLosses,
                winStreak  = newWinStreak,
                lossStreak = newLossStreak,
                updatedAt  = now,
            )
        }
        eloPort.saveAll(updated)

        val updatedMap = updated.associateBy { it.riotId }
        val histories = changes.map { (riotId, delta) ->
            val prev      = currentElos[riotId]
            val eloBefore = prev?.elo ?: INITIAL_ELO
            val eloAfter  = updatedMap[riotId]?.elo ?: eloBefore
            val won       = match.participants.firstOrNull { it.riotId == riotId }?.win ?: false
            PlayerEloHistory(
                riotId      = riotId,
                matchId     = match.matchId,
                eloBefore   = eloBefore,
                eloAfter    = eloAfter,
                delta       = delta,
                win         = won,
                gameCreation = match.gameCreation,
                createdAt   = now,
            )
        }
        eloHistoryPort.saveAll(histories)
        log.debug("Elo 업데이트 완료 — matchId=${match.matchId}, 대상=${updated.size}명")
    }

    /**
     * 팀별 Elo 변동량 계산
     *
     * 공식:
     *   delta = K(games) × (actual − expected) × streakMult
     *           + ALPHA × (perfScore − 0.5)
     *
     * ‧ K: 게임 수 기반 동적 팩터 (배치~정착)
     * ‧ expected: 표준 Elo 기대값 (400 divisor)
     * ‧ streakMult: 연승/연패 배율
     * ‧ perfScore: 포지션 가중 퍼포먼스 0.1~0.9 클리핑
     */
    private fun calcEloChanges(match: Match, eloMap: Map<String, PlayerElo>): Map<String, Double> {
        val teamA = match.participants.filter { it.teamId == 100 }
        val teamB = match.participants.filter { it.teamId == 200 }
        if (teamA.isEmpty() || teamB.isEmpty()) return emptyMap()

        val avgEloA = teamA.map { eloMap[it.riotId]?.elo ?: INITIAL_ELO }.average()
        val avgEloB = teamB.map { eloMap[it.riotId]?.elo ?: INITIAL_ELO }.average()

        // 표준 Elo 기대값 (Elo 차이 400당 10× 확률)
        val eA = 1.0 / (1 + 10.0.pow((avgEloB - avgEloA) / 400))
        val eB = 1.0 - eA

        val aWon       = teamA.any { it.win }
        val positionsA = assignPositions(teamA)
        val positionsB = assignPositions(teamB)

        val result = mutableMapOf<String, Double>()

        fun calcTeam(team: List<MatchParticipant>, expected: Double, won: Boolean, positions: Map<String, Position>) {
            for (p in team) {
                val prev       = eloMap[p.riotId]
                val k          = kFactor(prev?.games ?: 0)
                val mult       = streakMultiplier(prev?.winStreak ?: 0, prev?.lossStreak ?: 0, won)
                val pos        = positions[p.riotId] ?: Position.MID
                val perf       = calcPerformanceScore(p, team, pos).coerceIn(0.1, 0.9)
                val baseChange = k * ((if (won) 1.0 else 0.0) - expected) * mult
                result[p.riotId] = baseChange + ALPHA * (perf - 0.5)
            }
        }

        calcTeam(teamA, eA, aWon, positionsA)
        calcTeam(teamB, eB, !aWon, positionsB)
        return result
    }

    // ────────── 포지션 배정 ──────────

    private enum class Position { TOP, JUNGLE, MID, ADC, SUPPORT }

    /**
     * 팀 5명에게 포지션을 1:1로 배정한다.
     * 1차: lane/role 명시값 우선
     * 2차: BOTTOM 2명 → 딜+골드 높은 쪽 ADC, 낮은 쪽 SUPPORT
     * 3차: 나머지 미배정자 → 적합도 점수 기반 그리디
     */
    private fun assignPositions(team: List<MatchParticipant>): Map<String, Position> {
        val assigned = mutableMapOf<String, Position>()
        val taken    = mutableSetOf<Position>()
        val pending  = mutableListOf<MatchParticipant>()

        for (p in team) {
            val pos = primaryPosition(p)
            if (pos != null && pos !in taken) {
                assigned[p.riotId] = pos; taken.add(pos)
            } else {
                pending.add(p)
            }
        }

        val remaining = Position.entries.toMutableList().apply { removeAll(taken) }

        if (Position.ADC in remaining && Position.SUPPORT in remaining) {
            val bottomPending = pending.filter {
                it.lane == "BOTTOM" || it.role in listOf("DUO_CARRY", "DUO_SUPPORT", "DUO")
            }
            if (bottomPending.size == 2) {
                val adc = bottomPending.maxByOrNull { it.damage + it.gold }!!
                val sup = bottomPending.first { it.riotId != adc.riotId }
                assigned[adc.riotId] = Position.ADC;     remaining.remove(Position.ADC)
                assigned[sup.riotId] = Position.SUPPORT; remaining.remove(Position.SUPPORT)
                pending.removeAll(bottomPending.toSet())
            }
        }

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
        p.lane == "TOP"                                 -> Position.TOP
        p.lane == "JUNGLE"                              -> Position.JUNGLE
        p.lane == "MIDDLE"                              -> Position.MID
        p.lane == "BOTTOM" && p.role == "CARRY"         -> Position.ADC
        p.lane == "BOTTOM" && p.role == "DUO_CARRY"     -> Position.ADC
        p.lane == "BOTTOM" && p.role == "SUPPORT"       -> Position.SUPPORT
        p.lane == "BOTTOM" && p.role == "DUO_SUPPORT"   -> Position.SUPPORT
        else                                            -> null
    }

    private fun positionScore(p: MatchParticipant, pos: Position): Double = when (pos) {
        Position.TOP     -> p.cs * 0.35 + p.damage * 0.3 + p.totalDamageTaken * 0.25 + p.turretKills * 5.0
        Position.JUNGLE  -> p.neutralMinionsKilled * 2.5 + p.damageDealtToObjectives * 0.5
        Position.MID     -> p.damage * 0.6 + p.cs * 0.4
        Position.ADC     -> p.damage * 0.5 + p.gold * 0.3 + p.cs * 0.2
        Position.SUPPORT -> p.visionScore * 3.0 + p.timeCCingOthers * 1.5 + p.wardsPlaced * 2.0
    }

    // ────────── 퍼포먼스 점수 (0.0 ~ 1.0) ──────────

    /**
     * 포지션 가중치 기반 퍼포먼스 점수.
     * 결과는 호출부에서 [0.1, 0.9]로 클리핑되어 Elo 극단 변동을 방지한다.
     *
     * 지표:
     *   kdaRank     — 팀 내 KDA 순위 (0~1)
     *   kp          — 킬 관여율
     *   dmgShare    — 팀 내 딜 비율
     *   goldShare   — 팀 내 골드 비율
     *   objDmgShare — 팀 내 오브젝트 딜 비율 (TOP/JGL 중요)
     *   visionShare — 팀 내 시야 점수 비율 (SUP 중요)
     *   ccShare     — 팀 내 CC 시간 비율 (SUP 중요)
     */
    private fun calcPerformanceScore(
        p: MatchParticipant,
        team: List<MatchParticipant>,
        pos: Position,
    ): Double {
        val teamKills   = team.sumOf { it.kills }.coerceAtLeast(1)
        val teamDamage  = team.sumOf { it.damage }.coerceAtLeast(1)
        val teamGold    = team.sumOf { it.gold }.coerceAtLeast(1)
        val teamVision  = team.sumOf { it.visionScore }.coerceAtLeast(1)
        val teamObjDmg  = team.sumOf { it.damageDealtToObjectives }.coerceAtLeast(1)
        val teamCC      = team.sumOf { it.timeCCingOthers }.coerceAtLeast(1)

        val kda       = (p.kills + p.assists).toDouble() / maxOf(1, p.deaths)
        val teamKdas  = team.map { (it.kills + it.assists).toDouble() / maxOf(1, it.deaths) }
        val kdaMin    = teamKdas.min()
        val kdaMax    = teamKdas.max()
        val kdaRank   = if (kdaMax == kdaMin) 0.5 else (kda - kdaMin) / (kdaMax - kdaMin)

        val kp          = (p.kills + p.assists).toDouble() / teamKills
        val dmgShare    = p.damage.toDouble() / teamDamage
        val goldShare   = p.gold.toDouble() / teamGold
        val visionShare = p.visionScore.toDouble() / teamVision
        val objDmgShare = p.damageDealtToObjectives.toDouble() / teamObjDmg
        val ccShare     = p.timeCCingOthers.toDouble() / teamCC

        // 가중치 합계 = 1.0 (각 포지션 역할 반영)
        return when (pos) {
            Position.TOP     -> 0.35 * kdaRank + 0.25 * kp + 0.20 * dmgShare + 0.12 * goldShare + 0.08 * objDmgShare
            Position.JUNGLE  -> 0.30 * kdaRank + 0.28 * kp + 0.12 * dmgShare + 0.08 * goldShare + 0.22 * objDmgShare
            Position.MID     -> 0.34 * kdaRank + 0.24 * kp + 0.27 * dmgShare + 0.15 * goldShare
            Position.ADC     -> 0.25 * kdaRank + 0.18 * kp + 0.35 * dmgShare + 0.22 * goldShare
            Position.SUPPORT -> 0.18 * kdaRank + 0.20 * kp + 0.05 * dmgShare + 0.12 * goldShare + 0.30 * visionShare + 0.15 * ccShare
        }
    }
}
