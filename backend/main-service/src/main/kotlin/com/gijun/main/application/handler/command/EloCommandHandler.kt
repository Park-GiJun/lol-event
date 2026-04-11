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
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.service.PositionDetector
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
        // K-Factor: 게임 수 기반 변동폭 (배치 → 정착)
        // ═══════════════════════════════════════════════════
        fun kFactor(games: Int): Double = when {
            games < 10  -> 64.0
            games < 25  -> 48.0
            games < 50  -> 36.0
            else        -> 28.0
        }

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
    //  핵심 계산 — 고도화된 Elo 시스템 v2
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
        log.debug("Elo v2 업데이트 — matchId=${match.matchId}, 대상=${updated.size}명")
    }

    private fun processMatchInMemory(
        match: Match, eloCache: MutableMap<String, PlayerElo>,
        allHistories: MutableList<PlayerEloHistory>,
    ) {
        val participants = if (match.participants.any { it.assignedPosition.isBlank() }) {
            PositionDetector.assignPositionsToAll(match.participants)
        } else {
            match.participants
        }
        if (participants.size < 2) return
        val correctedMatch = match.copy(participants = participants.toMutableList())
        val currentElos = participants
            .mapNotNull { it.riotId.takeIf { r -> r.isNotBlank() } }
            .associateWith { eloCache[it] }.filterValues { it != null }.mapValues { it.value!! }.toMutableMap()

        val changes = calcEloChanges(correctedMatch, currentElos)
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

        // 포지션 배정
        val positionsA = PositionDetector.assignPositions(teamA)
        val positionsB = PositionDetector.assignPositions(teamB)
        val allPositions = positionsA + positionsB

        // 라인 상대 매핑 (같은 포지션끼리 매칭)
        val laneOpponents = buildLaneOpponentMap(teamA, teamB, positionsA, positionsB)

        // 이변 배율: 낮은 팀이 이기면 보너스
        val eloDiff = kotlin.math.abs(avgEloA - avgEloB)
        val upsetMult = if (eloDiff > 100) 1.0 + (eloDiff - 100) * 0.001 else 1.0 // 최대 ~1.3

        val result = mutableMapOf<String, Double>()

        fun calcForPlayer(p: MatchParticipant, team: List<MatchParticipant>, enemyTeam: List<MatchParticipant>,
                          expected: Double, won: Boolean) {
            val prev = eloMap[p.riotId]
            val k = kFactor(prev?.games ?: 0)
            val mult = streakMultiplier(prev?.winStreak ?: 0, prev?.lossStreak ?: 0, won)
            val pos = allPositions[p.riotId] ?: Position.MID

            // 1) 퍼포먼스 점수 (팀 내 기여도) — 0~1
            val perfScore = calcPerformanceScore(p, team, pos).coerceIn(0.05, 0.95)

            // 2) 라인전 점수 (상대 라이너와 직접 비교) — 0~1
            val opponent = laneOpponents[p.riotId]
            val laneScore = if (opponent != null) calcLaneScore(p, opponent, pos).coerceIn(0.05, 0.95) else 0.5

            // 3) 캐리/앵커 보너스
            val carryBonus = calcCarryBonus(p, team, enemyTeam, won)

            // 4) 멀티킬 보너스
            val multikillBonus = calcMultikillBonus(p)

            // 5) 목표 기여 보너스
            val objectiveBonus = calcObjectiveBonus(p, team)

            // ── 최종 공식 ──
            val baseChange = k * ((if (won) 1.0 else 0.0) - expected) * mult *
                (if ((won && expected < 0.4) || (!won && expected > 0.6)) upsetMult else 1.0)

            val perfAdj = 36.0 * (perfScore - 0.5)
            val laneAdj = 20.0 * (laneScore - 0.5)

            val totalDelta = baseChange + perfAdj + laneAdj + carryBonus + multikillBonus + objectiveBonus
            result[p.riotId] = totalDelta
        }

        teamA.forEach { calcForPlayer(it, teamA, teamB, eA, aWon) }
        teamB.forEach { calcForPlayer(it, teamB, teamA, eB, !aWon) }
        return result
    }

    // ═══════════════════════════════════════════════════════════
    //  퍼포먼스 점수 (팀 내 상대적 기여도) — 고도화 v2
    // ═══════════════════════════════════════════════════════════

    private fun calcPerformanceScore(p: MatchParticipant, team: List<MatchParticipant>, pos: Position): Double {
        val teamKills   = team.sumOf { it.kills }.coerceAtLeast(1)
        val teamDamage  = team.sumOf { it.damage }.coerceAtLeast(1)
        val teamGold    = team.sumOf { it.gold }.coerceAtLeast(1)
        val teamVision  = team.sumOf { it.visionScore }.coerceAtLeast(1)
        val teamObjDmg  = team.sumOf { it.damageDealtToObjectives }.coerceAtLeast(1)
        val teamCC      = team.sumOf { it.timeCCingOthers }.coerceAtLeast(1)
        val teamTankDmg = team.sumOf { it.totalDamageTaken }.coerceAtLeast(1)
        val teamHeal    = team.sumOf { it.totalHeal }.coerceAtLeast(1)
        val teamWards   = team.sumOf { it.wardsPlaced + it.wardsKilled }.coerceAtLeast(1)

        val kda = (p.kills + p.assists).toDouble() / maxOf(1, p.deaths)
        val teamKdas = team.map { (it.kills + it.assists).toDouble() / maxOf(1, it.deaths) }
        val kdaRank = normalizeInTeam(kda, teamKdas)

        val kp          = (p.kills + p.assists).toDouble() / teamKills
        val dmgShare    = p.damage.toDouble() / teamDamage
        val goldShare   = p.gold.toDouble() / teamGold
        val visionShare = p.visionScore.toDouble() / teamVision
        val objDmgShare = p.damageDealtToObjectives.toDouble() / teamObjDmg
        val ccShare     = p.timeCCingOthers.toDouble() / teamCC

        val tankShare   = p.totalDamageTaken.toDouble() / teamTankDmg
        val healShare   = p.totalHeal.toDouble() / teamHeal
        val wardShare   = (p.wardsPlaced + p.wardsKilled).toDouble() / teamWards

        val goldEff = p.damage.toDouble() / maxOf(1, p.gold)
        val teamGoldEffs = team.map { it.damage.toDouble() / maxOf(1, it.gold) }
        val goldEffRank = normalizeInTeam(goldEff, teamGoldEffs)

        val deathControl = 1.0 - (p.deaths.toDouble() / maxOf(1, team.maxOf { it.deaths }))

        return when (pos) {
            Position.TOP -> {
                0.20 * kdaRank + 0.15 * kp + 0.15 * dmgShare + 0.10 * goldShare +
                0.15 * tankShare + 0.08 * objDmgShare + 0.07 * goldEffRank + 0.05 * deathControl + 0.05 * ccShare
            }
            Position.JUNGLE -> {
                0.18 * kdaRank + 0.20 * kp + 0.10 * dmgShare + 0.05 * goldShare +
                0.22 * objDmgShare + 0.08 * wardShare + 0.07 * goldEffRank + 0.05 * deathControl + 0.05 * ccShare
            }
            Position.MID -> {
                0.20 * kdaRank + 0.18 * kp + 0.22 * dmgShare + 0.10 * goldShare +
                0.08 * goldEffRank + 0.07 * deathControl + 0.05 * objDmgShare + 0.05 * ccShare + 0.05 * visionShare
            }
            Position.ADC -> {
                0.15 * kdaRank + 0.12 * kp + 0.28 * dmgShare + 0.15 * goldShare +
                0.12 * goldEffRank + 0.08 * deathControl + 0.05 * objDmgShare + 0.05 * visionShare
            }
            Position.SUPPORT -> {
                0.12 * kdaRank + 0.18 * kp + 0.03 * dmgShare + 0.05 * goldShare +
                0.25 * wardShare + 0.15 * ccShare + 0.08 * healShare + 0.07 * deathControl + 0.07 * tankShare
            }
            Position.UNKNOWN -> {
                0.20 * kdaRank + 0.18 * kp + 0.22 * dmgShare + 0.10 * goldShare +
                0.08 * goldEffRank + 0.07 * deathControl + 0.05 * objDmgShare + 0.05 * ccShare + 0.05 * visionShare
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  라인전 점수 — 같은 포지션 상대와 직접 비교
    // ═══════════════════════════════════════════════════════════

    private fun calcLaneScore(me: MatchParticipant, opp: MatchParticipant, pos: Position): Double {
        fun compareMetric(myVal: Double, oppVal: Double): Double {
            val total = myVal + oppVal
            return if (total <= 0) 0.5 else myVal / total
        }

        val csAdv     = compareMetric(me.cs.toDouble(), opp.cs.toDouble())
        val goldAdv   = compareMetric(me.gold.toDouble(), opp.gold.toDouble())
        val dmgAdv    = compareMetric(me.damage.toDouble(), opp.damage.toDouble())
        val kdaMe     = (me.kills + me.assists).toDouble() / maxOf(1, me.deaths)
        val kdaOpp    = (opp.kills + opp.assists).toDouble() / maxOf(1, opp.deaths)
        val kdaAdv    = compareMetric(kdaMe, kdaOpp)
        val visionAdv = compareMetric(me.visionScore.toDouble(), opp.visionScore.toDouble())
        val objAdv    = compareMetric(me.damageDealtToObjectives.toDouble(), opp.damageDealtToObjectives.toDouble())
        val tankAdv   = compareMetric(me.totalDamageTaken.toDouble(), opp.totalDamageTaken.toDouble())

        return when (pos) {
            Position.TOP     -> 0.20 * kdaAdv + 0.20 * csAdv + 0.15 * goldAdv + 0.15 * dmgAdv + 0.20 * tankAdv + 0.10 * objAdv
            Position.JUNGLE  -> 0.25 * kdaAdv + 0.10 * csAdv + 0.15 * goldAdv + 0.10 * dmgAdv + 0.30 * objAdv + 0.10 * visionAdv
            Position.MID     -> 0.25 * kdaAdv + 0.20 * csAdv + 0.15 * goldAdv + 0.25 * dmgAdv + 0.10 * visionAdv + 0.05 * objAdv
            Position.ADC     -> 0.20 * kdaAdv + 0.20 * csAdv + 0.20 * goldAdv + 0.30 * dmgAdv + 0.10 * objAdv
            Position.SUPPORT -> 0.25 * kdaAdv + 0.05 * csAdv + 0.10 * goldAdv + 0.05 * dmgAdv + 0.30 * visionAdv + 0.15 * tankAdv + 0.10 * objAdv
            Position.UNKNOWN -> 0.25 * kdaAdv + 0.20 * csAdv + 0.15 * goldAdv + 0.25 * dmgAdv + 0.10 * visionAdv + 0.05 * objAdv
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  캐리/앵커 보너스
    // ═══════════════════════════════════════════════════════════

    private fun calcCarryBonus(p: MatchParticipant, team: List<MatchParticipant>,
                               enemyTeam: List<MatchParticipant>, won: Boolean): Double {
        val teamDmg = team.sumOf { it.damage }.coerceAtLeast(1)
        val teamDeaths = team.sumOf { it.deaths }.coerceAtLeast(1)
        val teamKills = team.sumOf { it.kills }.coerceAtLeast(1)
        val dmgShare = p.damage.toDouble() / teamDmg
        val deathShare = p.deaths.toDouble() / teamDeaths
        val killShare = p.kills.toDouble() / teamKills

        var bonus = 0.0

        if (won) {
            if (dmgShare >= 0.40) bonus += 4.0
            if (dmgShare >= 0.50) bonus += 4.0
            if (killShare >= 0.45) bonus += 2.0
            if (p.deaths == 0 && p.kills + p.assists >= 5) bonus += 2.0
        } else {
            if (deathShare >= 0.40) bonus -= 3.0
            if (deathShare >= 0.50) bonus -= 3.0
            if (dmgShare >= 0.35) bonus += 2.0
        }

        return bonus
    }

    // ═══════════════════════════════════════════════════════════
    //  멀티킬 보너스
    // ═══════════════════════════════════════════════════════════

    private fun calcMultikillBonus(p: MatchParticipant): Double {
        var bonus = 0.0
        bonus += p.pentaKills * 4.0
        bonus += p.quadraKills * 2.0
        bonus += p.tripleKills * 1.0
        if (p.largestKillingSpree >= 8) bonus += 2.0
        else if (p.largestKillingSpree >= 5) bonus += 1.0
        return bonus.coerceAtMost(6.0)
    }

    // ═══════════════════════════════════════════════════════════
    //  목표 기여 보너스
    // ═══════════════════════════════════════════════════════════

    private fun calcObjectiveBonus(p: MatchParticipant, team: List<MatchParticipant>): Double {
        var bonus = 0.0
        if (p.firstBloodKill) bonus += 1.5
        if (p.firstBloodAssist) bonus += 0.5
        if (p.firstTowerKill) bonus += 1.0
        if (p.firstTowerAssist) bonus += 0.3
        bonus += p.inhibitorKills * 0.5
        return bonus.coerceAtMost(4.0)
    }

    // ═══════════════════════════════════════════════════════════
    //  라인 상대 매핑
    // ═══════════════════════════════════════════════════════════

    private fun buildLaneOpponentMap(
        teamA: List<MatchParticipant>, teamB: List<MatchParticipant>,
        posA: Map<String, Position>, posB: Map<String, Position>,
    ): Map<String, MatchParticipant> {
        val result = mutableMapOf<String, MatchParticipant>()
        val posToA = posA.entries.groupBy({ it.value }, { it.key })
        val posToB = posB.entries.groupBy({ it.value }, { it.key })

        for (pos in Position.entries) {
            val aIds = posToA[pos] ?: continue
            val bIds = posToB[pos] ?: continue
            val aPlayer = teamA.firstOrNull { it.riotId in aIds } ?: continue
            val bPlayer = teamB.firstOrNull { it.riotId in bIds } ?: continue
            result[aPlayer.riotId] = bPlayer
            result[bPlayer.riotId] = aPlayer
        }
        return result
    }

    // ═══════════════════════════════════════════════════════════
    //  유틸리티
    // ═══════════════════════════════════════════════════════════

    private fun normalizeInTeam(value: Double, allValues: List<Double>): Double {
        val min = allValues.min()
        val max = allValues.max()
        return if (max == min) 0.5 else (value - min) / (max - min)
    }
}
