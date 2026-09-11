package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.stats.result.RecalculateResult
import com.gijun.main.application.port.`in`.CalculateRatingForMatchUseCase
import com.gijun.main.application.port.`in`.ResetAndRecalculateRatingUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.PlayerRatingPort
import com.gijun.main.application.port.out.RatingHistoryPort
import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.rating.PlayerRating
import com.gijun.main.domain.model.rating.RatingHistory
import com.gijun.main.domain.service.LaneScores
import com.gijun.main.domain.service.RatingEngine
import com.gijun.main.domain.service.RiotIdNormalizer
import com.gijun.main.domain.service.TimelineParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 레이팅 집계. 산식은 [RatingEngine] · [LaneScores] · [com.gijun.main.domain.service.RatingMath] 에 있고
 * 여기서는 "어떤 경기를 셀지"와 영속화만 다룬다.
 */
@Service
class RatingCommandHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val playerRatingPort: PlayerRatingPort,
    private val ratingHistoryPort: RatingHistoryPort,
    private val normalizer: RiotIdNormalizer,
) : CalculateRatingForMatchUseCase, ResetAndRecalculateRatingUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        /**
         * 타임라인을 몇 경기씩 묶어 읽을지. 경기당 60KB 급이라 500경기를 한 번에 들면
         * 30MB 가 힙에 얹힌다. 재집계는 한 번 도는 관리 작업이라 왕복 몇 번을 더 감수하는 쪽이 낫다.
         */
        const val TIMELINE_CHUNK = 50
    }

    // ────────── 경기 한 판 ──────────

    @Transactional
    override fun calculateForMatch(matchId: String) {
        val match = matchPersistencePort.findByMatchId(matchId)
            ?: run { log.warn("레이팅 계산 skip — 매치 없음: $matchId"); return }

        if (!RatingEngine.isRatable(match)) {
            log.info(
                "레이팅 계산 skip — 재생 대상 아님: $matchId " +
                    "(참가자 ${match.participants.size}명, ${match.gameDuration}초)"
            )
            return
        }

        // Kafka 는 최소 한 번 배달이다. 같은 매치가 두 번 오면 점수가 두 번 반영된다.
        if (ratingHistoryPort.existsByMatchId(matchId)) {
            log.info("레이팅 계산 skip — 이미 반영된 매치: $matchId")
            return
        }

        // Elo 는 순차 계산이라 경기 순서가 곧 결과다. 과거 경기가 뒤늦게 도착하면 그 경기만
        // 끼워 넣을 수 없고, 그 이후 전부가 다시 계산돼야 한다.
        val latest = ratingHistoryPort.findLatestGameCreation()
        if (latest != null && match.gameCreation < latest) {
            log.warn("레이팅 순서 역전 — $matchId (gameCreation=${match.gameCreation} < 최신=$latest). 전체 재집계로 전환한다.")
            resetAndRecalculate()
            return
        }

        val ids = normalizer.canonicalDistinct(match.participants.map { it.riotId }.filter { it.isNotBlank() })
        val current = playerRatingPort.findAllByRiotIds(ids).associateBy { it.riotId }

        val scored = scoreLanes(match, matchPersistencePort.findTimelineRaw(listOf(matchId))[matchId])
        val outcome = RatingEngine.rate(match, scored, current, normalizer::canonical)
            ?: run { log.warn("레이팅 계산 skip — 승패를 판정할 수 없다: $matchId"); return }

        playerRatingPort.saveAll(outcome.ratings)
        ratingHistoryPort.saveAll(outcome.histories)
        matchPersistencePort.updateLaneMethods(mapOf(matchId to outcome.method))

        log.debug(
            "레이팅 갱신 — matchId=$matchId, ${outcome.method}, " +
                "라인 대결 ${outcome.duelCount}쌍, 대상 ${outcome.ratings.size}명"
        )
    }

    // ────────── 전체 재집계 ──────────

    /**
     * 전체 매치를 gameCreation 오름차순으로 재생한다.
     *
     * 경기마다 그 경기에 저장된 데이터로 판정 방법이 자동으로 정해지므로, 과거(LEGACY_FINAL)와
     * 신규(TIMELINE_15)가 한 레이팅 안에 섞여도 문제없다. Elo 는 **매 경기 내부에서만** 비교하기
     * 때문이다. 방법이 달라 점수의 단위가 달라도 승자 판정 결과는 같은 뜻을 가진다.
     * 억지로 스케일을 맞추려 하지 마라.
     */
    @Transactional
    override fun resetAndRecalculate(): RecalculateResult {
        log.info("레이팅 전체 초기화 시작")
        playerRatingPort.deleteAll()
        ratingHistoryPort.deleteAll()

        val all = matchPersistencePort.findAllOrderedByGameCreation()
        val ratable = all.filter(RatingEngine::isRatable)

        val ratings = mutableMapOf<String, PlayerRating>()
        val histories = mutableListOf<RatingHistory>()
        val methods = mutableMapOf<String, LaneMethod>()
        var counted = 0
        var duels = 0

        for (chunk in ratable.chunked(TIMELINE_CHUNK)) {
            val raws = matchPersistencePort.findTimelineRaw(chunk.map { it.matchId })
            for (match in chunk) {
                val scored = scoreLanes(match, raws[match.matchId])
                val outcome = RatingEngine.rate(match, scored, ratings, normalizer::canonical) ?: continue
                outcome.ratings.forEach { ratings[it.riotId] = it }
                histories += outcome.histories
                methods[match.matchId] = outcome.method
                duels += outcome.duelCount
                counted++
            }
        }

        playerRatingPort.saveAll(ratings.values.toList())
        ratingHistoryPort.saveAll(histories)
        matchPersistencePort.updateLaneMethods(methods)

        val result = RecalculateResult(
            totalMatches = all.size,
            ratedMatches = counted,
            players = ratings.size,
            laneDuels = duels,
            methodCounts = methods.values.groupingBy { it.name }.eachCount(),
        )
        log.info("레이팅 재집계 완료 — $result")
        return result
    }

    // ────────── 공통 ──────────

    private fun scoreLanes(match: Match, timelineRaw: String?): LaneScores.Scored =
        LaneScores.of(match, TimelineParser.parse(timelineRaw))
}
