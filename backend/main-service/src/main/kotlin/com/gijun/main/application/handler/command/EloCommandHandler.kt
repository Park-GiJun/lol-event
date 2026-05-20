package com.gijun.main.application.handler.command

import com.gijun.main.application.port.`in`.CalculateEloForMatchUseCase
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
class EloCommandHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val eloPort: EloPort,
    private val eloHistoryPort: EloHistoryPort,
) : CalculateEloForMatchUseCase, ResetAndRecalculateEloUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val INITIAL_ELO = 1000.0
        private const val MIN_ELO     = 100.0
        private const val ARAM_QUEUE_ID = 3270

        // ═══════════════════════════════════════════════════
        // 고정 K값 (게임 수와 무관한 일정 변동폭, 표준 Elo)
        // ═══════════════════════════════════════════════════
        private const val K_FACTOR = 32.0

        // ═══════════════════════════════════════════════════
        // 이변 배율: 팀 평균 ELO 차이가 클수록, 약팀이 이기면 보너스
        //   diff <= THRESHOLD          → 1.0 (배율 없음)
        //   diff  > THRESHOLD          → 1.0 + (diff - THRESHOLD) * SLOPE, 상한 MAX
        //   (약 30 ~ 116 구간에서 1.0 → 1.3 으로 스케일)
        // ═══════════════════════════════════════════════════
        private const val UPSET_THRESHOLD = 30.0
        private const val UPSET_SLOPE     = 0.0035
        private const val UPSET_MAX       = 1.3

        // ═══════════════════════════════════════════════════
        // 연승/연패 배율
        // ═══════════════════════════════════════════════════
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
        if (match.queueId == ARAM_QUEUE_ID) {
            log.info("Elo 계산 skip — 칼바람: $matchId")
            return
        }
        processMatch(match)
    }

    @Transactional
    override fun resetAndRecalculate() {
        log.info("Elo 전체 초기화 시작")
        eloPort.deleteAll()
        eloHistoryPort.deleteAll()
        val matches = matchPersistencePort.findAllOrderedByGameCreation()
            .filter { it.queueId != ARAM_QUEUE_ID }
        log.info("재집계 대상 매치: ${matches.size}개 (칼바람 제외)")

        val eloCache = mutableMapOf<String, PlayerElo>()
        val allHistories = mutableListOf<PlayerEloHistory>()

        for (match in matches) {
            processMatchInMemory(match, eloCache, allHistories)
        }

        eloPort.saveAll(eloCache.values.toList())
        eloHistoryPort.saveAll(allHistories)
        log.info("Elo 재집계 완료")
    }

    // ═══════════════════════════════════════════════════════════
    //  핵심 계산 — 순수 Elo (승패 + 이변배율 + 연승/연패)
    // ═══════════════════════════════════════════════════════════

    private fun processMatch(match: Match) {
        val participants = match.participants
        if (participants.size < 2) return

        val riotIds = participants.mapNotNull { it.riotId.takeIf { r -> r.isNotBlank() } }
        val currentElos = eloPort.findAllByRiotIds(riotIds).associate { it.riotId to it }.toMutableMap()

        val changes = calcEloChanges(match, currentElos)

        val now = LocalDateTime.now()
        val updated = changes.map { (riotId, delta) ->
            val prev = currentElos[riotId]
            val participant = match.participants.firstOrNull { it.riotId == riotId }
            val won = participant?.win ?: false
            val newElo = maxOf(MIN_ELO, (prev?.elo ?: INITIAL_ELO) + delta)
            PlayerElo(
                id = prev?.id ?: 0, riotId = riotId, elo = newElo,
                games = (prev?.games ?: 0) + 1,
                wins = (prev?.wins ?: 0) + if (won) 1 else 0,
                losses = (prev?.losses ?: 0) + if (!won) 1 else 0,
                winStreak = if (won) (prev?.winStreak ?: 0) + 1 else 0,
                lossStreak = if (!won) (prev?.lossStreak ?: 0) + 1 else 0,
                updatedAt = now,
            )
        }
        eloPort.saveAll(updated)

        val updatedMap = updated.associateBy { it.riotId }
        val histories = changes.map { (riotId, delta) ->
            val prev = currentElos[riotId]
            val eloBefore = prev?.elo ?: INITIAL_ELO
            PlayerEloHistory(
                riotId = riotId, matchId = match.matchId,
                eloBefore = eloBefore, eloAfter = updatedMap[riotId]?.elo ?: eloBefore,
                delta = delta, win = match.participants.firstOrNull { it.riotId == riotId }?.win ?: false,
                gameCreation = match.gameCreation, createdAt = now,
            )
        }
        eloHistoryPort.saveAll(histories)
        log.debug("Elo 업데이트 — matchId=${match.matchId}, 대상=${updated.size}명")
    }

    private fun processMatchInMemory(
        match: Match, eloCache: MutableMap<String, PlayerElo>,
        allHistories: MutableList<PlayerEloHistory>,
    ) {
        val participants = match.participants
        if (participants.size < 2) return
        val currentElos = participants
            .mapNotNull { it.riotId.takeIf { r -> r.isNotBlank() } }
            .associateWith { eloCache[it] }.filterValues { it != null }.mapValues { it.value!! }.toMutableMap()

        val changes = calcEloChanges(match, currentElos)
        val now = LocalDateTime.now()

        changes.forEach { (riotId, delta) ->
            val prev = currentElos[riotId]
            val won = participants.firstOrNull { it.riotId == riotId }?.win ?: false
            val newElo = maxOf(MIN_ELO, (prev?.elo ?: INITIAL_ELO) + delta)
            val updated = PlayerElo(
                id = prev?.id ?: 0, riotId = riotId, elo = newElo,
                games = (prev?.games ?: 0) + 1,
                wins = (prev?.wins ?: 0) + if (won) 1 else 0,
                losses = (prev?.losses ?: 0) + if (!won) 1 else 0,
                winStreak = if (won) (prev?.winStreak ?: 0) + 1 else 0,
                lossStreak = if (!won) (prev?.lossStreak ?: 0) + 1 else 0,
                updatedAt = now,
            )
            eloCache[riotId] = updated
            allHistories.add(PlayerEloHistory(
                riotId = riotId, matchId = match.matchId,
                eloBefore = prev?.elo ?: INITIAL_ELO, eloAfter = newElo,
                delta = delta, win = won, gameCreation = match.gameCreation, createdAt = now,
            ))
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Elo 변동량 계산 — 핵심 알고리즘
    // ═══════════════════════════════════════════════════════════

    private fun calcEloChanges(match: Match, eloMap: Map<String, PlayerElo>): Map<String, Double> {
        val teamA = match.participants.filter { it.teamId == 100 }
        val teamB = match.participants.filter { it.teamId == 200 }
        if (teamA.isEmpty() || teamB.isEmpty()) return emptyMap()

        val avgEloA = teamA.map { eloMap[it.riotId]?.elo ?: INITIAL_ELO }.average()
        val avgEloB = teamB.map { eloMap[it.riotId]?.elo ?: INITIAL_ELO }.average()

        val eA = 1.0 / (1 + 10.0.pow((avgEloB - avgEloA) / 400))
        val eB = 1.0 - eA
        val aWon = teamA.any { it.win }

        // 이변 배율: 약팀이 이긴 경우에만 적용
        val eloDiff = kotlin.math.abs(avgEloA - avgEloB)
        val upsetMult = if (eloDiff > UPSET_THRESHOLD)
            (1.0 + (eloDiff - UPSET_THRESHOLD) * UPSET_SLOPE).coerceAtMost(UPSET_MAX)
        else 1.0

        val result = mutableMapOf<String, Double>()

        fun calcForPlayer(p: MatchParticipant, expected: Double, won: Boolean) {
            val prev = eloMap[p.riotId]
            val mult = streakMultiplier(prev?.winStreak ?: 0, prev?.lossStreak ?: 0, won)
            // 약팀(expected < 0.5)이 이겼을 때만 이변 배율 부여
            val upset = if (won && expected < 0.5) upsetMult else 1.0

            result[p.riotId] = K_FACTOR * ((if (won) 1.0 else 0.0) - expected) * mult * upset
        }

        teamA.forEach { calcForPlayer(it, eA, aWon) }
        teamB.forEach { calcForPlayer(it, eB, !aWon) }
        return result
    }
}
