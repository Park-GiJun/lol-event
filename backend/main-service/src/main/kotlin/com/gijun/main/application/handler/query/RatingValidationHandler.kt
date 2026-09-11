package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.PredictionMetrics
import com.gijun.main.application.dto.stats.result.RatingValidationResult
import com.gijun.main.application.dto.stats.result.SplitHalfReliability
import com.gijun.main.application.dto.stats.result.ValidationScope
import com.gijun.main.application.port.`in`.ValidateRatingUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.rating.LaneResult
import com.gijun.main.domain.model.rating.PlayerRating
import com.gijun.main.domain.service.LaneScores
import com.gijun.main.domain.service.RatingEngine
import com.gijun.main.domain.service.RatingMath
import com.gijun.main.domain.service.RiotIdNormalizer
import com.gijun.main.domain.service.TimelineParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * 워크포워드 검증. **저장된 레이팅을 건드리지 않는다** — 메모리에서 처음부터 다시 재생한다.
 *
 * ## 왜 세션 경계를 세나
 * 이 내전은 하루 저녁에 여러 판을 몰아서 한다. 세션 안에서는 같은 편성이 반복되기 쉬워
 * "직전 경기에서 이긴 팀이 또 이긴다"가 실력이 아니라 그냥 같은 팀이라서 맞는다.
 * 그래서 6시간 이상 끊긴 지점을 세션 경계로 보고, 세션 첫 경기만 따로 집계한 값을 함께 낸다.
 *
 * ## 왜 팀 연속 경기를 빼나
 * 전체 108쌍 중 16쌍이 "직전 경기와 팀 구성이 같거나 진영만 바뀐" 경우였고, 그 경우
 * 직전 승자가 69% 확률로 재승리했다. 제외하지 않으면 **K 가 클수록 좋아 보이는 착시**가 생긴다.
 * 실제로 이 함정에 한 번 빠졌다. 기본값을 켜 두는 이유다.
 */
@Service
@Transactional(readOnly = true)
class RatingValidationHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val normalizer: RiotIdNormalizer,
) : ValidateRatingUseCase {

    private companion object {
        /** 이 간격 이상 끊기면 새 세션. */
        const val SESSION_GAP_MS = 6L * 60 * 60 * 1000

        /** 반분 신뢰도에서 한쪽 반이 이 수 미만이면 그 사람은 뺀다. */
        const val MIN_DUELS_PER_HALF = 5

        const val TIMELINE_CHUNK = 50
    }

    override fun validate(warmup: Int, excludeRepeatedTeams: Boolean): RatingValidationResult {
        val all = matchPersistencePort.findAllOrderedByGameCreation()
        val matches = all.filter(RatingEngine::isRatable)

        val ratings = mutableMapOf<String, PlayerRating>()
        val overall = ScopeAccumulator()
        val sessionFirst = ScopeAccumulator()
        val duelsByPlayer = mutableMapOf<String, MutableList<Boolean>>()
        val methodCounts = mutableMapOf<String, Int>()

        var sessions = 0
        var excluded = 0
        var index = 0
        var previous: Match? = null

        for (chunk in matches.chunked(TIMELINE_CHUNK)) {
            val raws = matchPersistencePort.findTimelineRaw(chunk.map { it.matchId })
            for (match in chunk) {
                // 승패를 판정할 수 없는 경기는 예측 대상도, 세션 계산 대상도 아니다.
                val blueWon = blueWon(match) ?: continue

                val prev = previous
                val newSession = prev == null || match.gameCreation - prev.gameCreation >= SESSION_GAP_MS
                if (newSession) sessions++
                val repeated = prev != null && sameTeams(match, prev)

                // ── 예측은 반영 **전** 레이팅으로만 ──
                if (index >= warmup) {
                    if (excludeRepeatedTeams && repeated) {
                        excluded++
                    } else {
                        val lane = blueProbability(match, ratings) { it.laneElo }
                        val team = blueProbability(match, ratings) { it.teamElo }
                        overall.add(blueWon, lane, team)
                        if (newSession) sessionFirst.add(blueWon, lane, team)
                    }
                }

                // ── 반영 ──
                val scored = LaneScores.of(match, TimelineParser.parse(raws[match.matchId]))
                val outcome = RatingEngine.rate(match, scored, ratings, normalizer::canonical)
                if (outcome != null) {
                    outcome.ratings.forEach { ratings[it.riotId] = it }
                    outcome.histories.forEach { h ->
                        when (h.laneResult) {
                            LaneResult.WIN -> duelsByPlayer.getOrPut(h.riotId) { mutableListOf() }.add(true)
                            LaneResult.LOSS -> duelsByPlayer.getOrPut(h.riotId) { mutableListOf() }.add(false)
                            LaneResult.NONE -> Unit
                        }
                    }
                    methodCounts.merge(outcome.method.name, 1, Int::plus)
                }

                previous = match
                index++
            }
        }

        return RatingValidationResult(
            totalMatches = all.size,
            ratableMatches = matches.size,
            warmup = warmup,
            sessions = sessions,
            excludedRepeatedTeams = excluded,
            methodCounts = methodCounts,
            overall = overall.toScope(),
            sessionFirst = sessionFirst.toScope(),
            splitHalf = splitHalf(duelsByPlayer),
        )
    }

    // ────────── 예측 ──────────

    /**
     * 블루가 이길 기대 확률. [select] 로 어느 레이팅을 쓸지 고른다 — 두 레이팅을 섞지 않는다.
     *
     * 수축값이 아니라 **원값**으로 예측한다. 표시용 수축을 예측에 되먹이면 검증에서 더 나빴다.
     */
    private fun blueProbability(
        match: Match,
        ratings: Map<String, PlayerRating>,
        select: (PlayerRating) -> Double,
    ): Double {
        fun avg(teamId: Int) = match.participants
            .filter { it.teamId == teamId && it.riotId.isNotBlank() }
            .map { p ->
                val r = ratings[normalizer.canonical(p.riotId)]
                if (r == null) RatingMath.START else select(r)
            }
            .ifEmpty { listOf(RatingMath.START) }
            .average()

        return RatingMath.expected(avg(RatingEngine.TEAM_BLUE), avg(RatingEngine.TEAM_RED))
    }

    private fun blueWon(match: Match): Boolean? {
        val blue = match.participants.filter { it.teamId == RatingEngine.TEAM_BLUE }
        val red = match.participants.filter { it.teamId == RatingEngine.TEAM_RED }
        if (blue.isEmpty() || red.isEmpty()) return null
        val b = blue.any { it.win }
        val r = red.any { it.win }
        return if (b == r) null else b
    }

    /** 직전 경기와 같은 분할인가. 진영만 바뀐 경우도 같은 것으로 본다. */
    private fun sameTeams(a: Match, b: Match): Boolean {
        fun side(m: Match, teamId: Int) = m.participants
            .filter { it.teamId == teamId && it.riotId.isNotBlank() }
            .map { normalizer.canonical(it.riotId) }
            .toSet()

        val aBlue = side(a, RatingEngine.TEAM_BLUE)
        val aRed = side(a, RatingEngine.TEAM_RED)
        if (aBlue.isEmpty() || aRed.isEmpty()) return false
        val bBlue = side(b, RatingEngine.TEAM_BLUE)
        val bRed = side(b, RatingEngine.TEAM_RED)
        return (aBlue == bBlue && aRed == bRed) || (aBlue == bRed && aRed == bBlue)
    }

    // ────────── 집계 ──────────

    private class ScopeAccumulator {
        private var games = 0
        private var baseLoss = 0.0
        private var baseHits = 0.0
        private var laneLoss = 0.0
        private var laneHits = 0.0
        private var teamLoss = 0.0
        private var teamHits = 0.0

        fun add(blueWon: Boolean, laneP: Double, teamP: Double) {
            games++
            baseLoss += logLoss(blueWon, 0.5); baseHits += hit(blueWon, 0.5)
            laneLoss += logLoss(blueWon, laneP); laneHits += hit(blueWon, laneP)
            teamLoss += logLoss(blueWon, teamP); teamHits += hit(blueWon, teamP)
        }

        fun toScope() = ValidationScope(
            games = games,
            baseline = metrics(baseLoss, baseHits),
            laneElo = metrics(laneLoss, laneHits),
            teamElo = metrics(teamLoss, teamHits),
        )

        private fun metrics(loss: Double, hits: Double) = PredictionMetrics(
            logLoss = if (games == 0) 0.0 else round4(loss / games),
            accuracy = if (games == 0) 0.0 else round4(hits / games),
        )

        private fun logLoss(y: Boolean, p: Double): Double {
            val clamped = p.coerceIn(EPS, 1 - EPS)
            return if (y) -ln(clamped) else -ln(1 - clamped)
        }

        /** 정확히 0.5 면 동전 던지기라 절반만 맞은 것으로 센다. */
        private fun hit(y: Boolean, p: Double): Double = when {
            p == 0.5 -> 0.5
            (p > 0.5) == y -> 1.0
            else -> 0.0
        }

        private fun round4(v: Double) = Math.round(v * 10_000) / 10_000.0

        private companion object {
            /** log(0) 방어. */
            const val EPS = 1e-9
        }
    }

    // ────────── 반분 신뢰도 ──────────

    private fun splitHalf(duels: Map<String, List<Boolean>>): SplitHalfReliability {
        val pairs = duels.values.mapNotNull { results ->
            val odd = results.filterIndexed { i, _ -> i % 2 == 0 }
            val even = results.filterIndexed { i, _ -> i % 2 == 1 }
            if (odd.size < MIN_DUELS_PER_HALF || even.size < MIN_DUELS_PER_HALF) null
            else odd.count { it }.toDouble() / odd.size to even.count { it }.toDouble() / even.size
        }

        val r = pearson(pairs)
        return SplitHalfReliability(
            players = pairs.size,
            minDuelsPerHalf = MIN_DUELS_PER_HALF,
            correlation = round3(r),
            // 반쪽짜리 측정을 전체 길이로 환산한다. r = -1 이면 분모가 0이라 그때는 보정하지 않는다.
            spearmanBrown = if (r <= -1.0) 0.0 else round3(2 * r / (1 + r)),
        )
    }

    private fun pearson(pairs: List<Pair<Double, Double>>): Double {
        if (pairs.size < 2) return 0.0
        val mx = pairs.sumOf { it.first } / pairs.size
        val my = pairs.sumOf { it.second } / pairs.size
        var sxy = 0.0
        var sxx = 0.0
        var syy = 0.0
        for ((x, y) in pairs) {
            val dx = x - mx
            val dy = y - my
            sxy += dx * dy
            sxx += dx * dx
            syy += dy * dy
        }
        val denom = sqrt(sxx * syy)
        return if (denom <= 0.0) 0.0 else sxy / denom
    }

    private fun round3(v: Double) = Math.round(v * 1000) / 1000.0
}
