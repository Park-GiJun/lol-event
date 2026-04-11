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
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

@Service
class EloCalculationHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val eloPort: EloPort,
    private val eloHistoryPort: EloHistoryPort,
) : CalculateEloForMatchUseCase, ResetAndRecalculateEloUseCase, GetEloUseCase, GetEloLeaderboardUseCase, GetEloHistoryUseCase {

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
                    rank = idx + 1, riotId = elo.riotId, elo = elo.elo,
                    games = elo.games, wins = elo.wins, losses = elo.losses,
                    winRate = (wr * 10).toLong() / 10.0,
                    winStreak = elo.winStreak, lossStreak = elo.lossStreak,
                )
            }
        )
    }

    // ═══════════════════════════════════════════════════════════
    //  핵심 계산 — 고도화된 Elo 시스템 v2
    //
    //  공식:
    //    delta = K × (actual − expected) × streakMult × upsetMult
    //            + PERF_WEIGHT × (performanceScore − 0.5)
    //            + LANE_WEIGHT × (laneScore − 0.5)
    //            + CARRY_BONUS
    //
    //  performanceScore: 포지션별 가중치 (팀 내 상대적 기여도)
    //  laneScore: 라인 상대와의 직접 비교 (같은 포지션)
    //  carryBonus: 1인 캐리 or 앵커 감지
    //  upsetMult: 이변 보너스/패널티
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
        val participants = match.participants
        if (participants.size < 2) return
        val currentElos = participants
            .mapNotNull { it.riotId.takeIf { r -> r.isNotBlank() } }
            .associateWith { eloCache[it] }.filterValues { it != null }.mapValues { it.value!! }.toMutableMap()

        val changes = calcEloChanges(match, currentElos)
        val now = LocalDateTime.now()

        changes.forEach { (riotId, delta) ->
            val prev = currentElos[riotId]
            val won = match.participants.firstOrNull { it.riotId == riotId }?.win ?: false
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
        val positionsA = assignPositions(teamA)
        val positionsB = assignPositions(teamB)
        val allPositions = positionsA + positionsB

        // 라인 상대 매핑 (같은 포지션끼리 매칭)
        val laneOpponents = buildLaneOpponentMap(teamA, teamB, positionsA, positionsB)

        // 이변 배율: 낮은 팀이 이기면 보너스
        val eloDiff = kotlin.math.abs(avgEloA - avgEloB)
        val upsetMult = if (eloDiff > 100) 1.0 + (eloDiff - 100) * 0.001 else 1.0 // 최대 ~1.3

        val result = mutableMapOf<String, Double>()
        val allParticipants = teamA + teamB

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
            // 기본: K × (실제 − 기대) × 연승배율 × 이변배율
            val baseChange = k * ((if (won) 1.0 else 0.0) - expected) * mult *
                (if ((won && expected < 0.4) || (!won && expected > 0.6)) upsetMult else 1.0)

            // 퍼포먼스 보정: 최대 ±18점 (팀 내 기여도)
            val perfAdj = 36.0 * (perfScore - 0.5)

            // 라인전 보정: 최대 ±10점 (상대 라이너 대비)
            val laneAdj = 20.0 * (laneScore - 0.5)

            // 캐리 보너스: 최대 ±8점
            // 멀티킬 보너스: 최대 +6점
            // 목표 기여 보너스: 최대 ±4점

            val totalDelta = baseChange + perfAdj + laneAdj + carryBonus + multikillBonus + objectiveBonus
            result[p.riotId] = totalDelta
        }

        teamA.forEach { calcForPlayer(it, teamA, teamB, eA, aWon) }
        teamB.forEach { calcForPlayer(it, teamB, teamA, eB, !aWon) }
        return result
    }

    // ═══════════════════════════════════════════════════════════
    //  퍼포먼스 점수 (팀 내 상대적 기여도) — 고도화 v2
    //
    //  7개 기본 지표 + 3개 신규 지표 = 10개 지표
    //  포지션별 가중치로 합산
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

        // KDA 랭킹 (팀 내 순위 → 0~1 정규화)
        val kda = (p.kills + p.assists).toDouble() / maxOf(1, p.deaths)
        val teamKdas = team.map { (it.kills + it.assists).toDouble() / maxOf(1, it.deaths) }
        val kdaRank = normalizeInTeam(kda, teamKdas)

        // 기본 Share 지표
        val kp          = (p.kills + p.assists).toDouble() / teamKills
        val dmgShare    = p.damage.toDouble() / teamDamage
        val goldShare   = p.gold.toDouble() / teamGold
        val visionShare = p.visionScore.toDouble() / teamVision
        val objDmgShare = p.damageDealtToObjectives.toDouble() / teamObjDmg
        val ccShare     = p.timeCCingOthers.toDouble() / teamCC

        // 신규 지표
        val tankShare   = p.totalDamageTaken.toDouble() / teamTankDmg   // 탱킹 기여
        val healShare   = p.totalHeal.toDouble() / teamHeal             // 힐링 기여
        val wardShare   = (p.wardsPlaced + p.wardsKilled).toDouble() / teamWards  // 시야 전체 기여

        // 골드 효율 (딜/골드 비율의 팀 내 순위)
        val goldEff = p.damage.toDouble() / maxOf(1, p.gold)
        val teamGoldEffs = team.map { it.damage.toDouble() / maxOf(1, it.gold) }
        val goldEffRank = normalizeInTeam(goldEff, teamGoldEffs)

        // 데스 절제력 (데스 적을수록 높음)
        val deathControl = 1.0 - (p.deaths.toDouble() / maxOf(1, team.maxOf { it.deaths }))

        // 포지션별 가중치 (합계 = 1.0)
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
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  라인전 점수 — 같은 포지션 상대와 직접 비교
    //
    //  CS 차이, 골드 차이, 딜 차이, KDA 비교, 시야 비교
    //  게임 시간 보정 (분당 환산)
    // ═══════════════════════════════════════════════════════════

    private fun calcLaneScore(me: MatchParticipant, opp: MatchParticipant, pos: Position): Double {
        // 각 지표에서 상대 대비 우위도 계산 (0~1, 0.5가 동등)
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

        // 포지션별 가중치 (합계 = 1.0)
        return when (pos) {
            Position.TOP     -> 0.20 * kdaAdv + 0.20 * csAdv + 0.15 * goldAdv + 0.15 * dmgAdv + 0.20 * tankAdv + 0.10 * objAdv
            Position.JUNGLE  -> 0.25 * kdaAdv + 0.10 * csAdv + 0.15 * goldAdv + 0.10 * dmgAdv + 0.30 * objAdv + 0.10 * visionAdv
            Position.MID     -> 0.25 * kdaAdv + 0.20 * csAdv + 0.15 * goldAdv + 0.25 * dmgAdv + 0.10 * visionAdv + 0.05 * objAdv
            Position.ADC     -> 0.20 * kdaAdv + 0.20 * csAdv + 0.20 * goldAdv + 0.30 * dmgAdv + 0.10 * objAdv
            Position.SUPPORT -> 0.25 * kdaAdv + 0.05 * csAdv + 0.10 * goldAdv + 0.05 * dmgAdv + 0.30 * visionAdv + 0.15 * tankAdv + 0.10 * objAdv
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  캐리/앵커 보너스
    //
    //  팀 내 딜 비중 40%+ 이면서 이기면 캐리 보너스
    //  팀 내 데스 비중 40%+ 이면서 지면 앵커 패널티
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
            // 캐리 보너스: 딜 비중이 높으면서 승리
            if (dmgShare >= 0.40) bonus += 4.0
            if (dmgShare >= 0.50) bonus += 4.0  // 총 +8 가능
            // 킬 주도: 킬 비중이 높으면 추가
            if (killShare >= 0.45) bonus += 2.0
            // 죽지 않고 이기면 추가
            if (p.deaths == 0 && p.kills + p.assists >= 5) bonus += 2.0
        } else {
            // 앵커 패널티: 데스 비중이 높으면서 패배
            if (deathShare >= 0.40) bonus -= 3.0
            if (deathShare >= 0.50) bonus -= 3.0  // 총 -6 가능
            // 패배 시에도 딜량이 높으면 방어적 보정
            if (dmgShare >= 0.35) bonus += 2.0
        }

        return bonus
    }

    // ═══════════════════════════════════════════════════════════
    //  멀티킬 보너스 — 더블킬~펜타킬 가산
    // ═══════════════════════════════════════════════════════════

    private fun calcMultikillBonus(p: MatchParticipant): Double {
        var bonus = 0.0
        bonus += p.pentaKills * 4.0
        bonus += p.quadraKills * 2.0
        bonus += p.tripleKills * 1.0
        // 최대 킬링 스프리 보너스
        if (p.largestKillingSpree >= 8) bonus += 2.0
        else if (p.largestKillingSpree >= 5) bonus += 1.0
        return bonus.coerceAtMost(6.0)
    }

    // ═══════════════════════════════════════════════════════════
    //  목표 기여 보너스 — 퍼스트블러드, 퍼스트타워 등
    // ═══════════════════════════════════════════════════════════

    private fun calcObjectiveBonus(p: MatchParticipant, team: List<MatchParticipant>): Double {
        var bonus = 0.0
        if (p.firstBloodKill) bonus += 1.5
        if (p.firstBloodAssist) bonus += 0.5
        if (p.firstTowerKill) bonus += 1.0
        if (p.firstTowerAssist) bonus += 0.3
        // 억제기 파괴
        bonus += p.inhibitorKills * 0.5
        return bonus.coerceAtMost(4.0)
    }

    // ═══════════════════════════════════════════════════════════
    //  라인 상대 매핑 — 같은 포지션끼리 매칭
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

    /** 팀 내 값 정규화 (0=최하, 1=최상, 전원 동일하면 0.5) */
    private fun normalizeInTeam(value: Double, allValues: List<Double>): Double {
        val min = allValues.min()
        val max = allValues.max()
        return if (max == min) 0.5 else (value - min) / (max - min)
    }

    // ────────── 포지션 배정 ──────────

    private enum class Position { TOP, JUNGLE, MID, ADC, SUPPORT }

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
            pending.remove(best); remaining.remove(pos)
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
}
