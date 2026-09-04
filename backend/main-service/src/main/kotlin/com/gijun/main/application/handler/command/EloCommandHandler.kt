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
import com.gijun.main.domain.service.EloRating
import com.gijun.main.domain.service.LanePerformance
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Elo 집계. 계산식 자체는 [EloRating] 에 있고 여기서는 "어떤 경기를 셀지"와 영속화만 다룬다.
 */
@Service
class EloCommandHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val eloPort: EloPort,
    private val eloHistoryPort: EloHistoryPort,
) : CalculateEloForMatchUseCase, ResetAndRecalculateEloUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val ARAM_QUEUE_ID = 3270
        private const val TEAM_A = 100
        private const val TEAM_B = 200

        /** 이 시간 안에 끝난 경기는 리메이크로 본다. gameDuration 은 초 단위다. */
        private const val REMAKE_DURATION_SEC = 300
    }

    // ────────── UseCase 구현 ──────────

    @Transactional
    override fun calculateForMatch(matchId: String) {
        val match = matchPersistencePort.findByMatchId(matchId)
            ?: run { log.warn("Elo 계산 skip — 매치 없음: $matchId"); return }

        skipReason(match)?.let { log.info("Elo 계산 skip — $it: $matchId"); return }

        // Kafka 는 최소 한 번 배달이다. 같은 매치가 두 번 오면 점수가 두 번 반영된다.
        if (eloHistoryPort.existsByMatchId(matchId)) {
            log.info("Elo 계산 skip — 이미 반영된 매치: $matchId")
            return
        }

        // Elo 는 순차 계산이라 경기 순서가 곧 결과다. 과거 경기가 뒤늦게 도착하면
        // 그 경기만 끼워 넣을 수 없고, 그 이후 전부가 다시 계산돼야 한다.
        val latest = eloHistoryPort.findLatestGameCreation()
        if (latest != null && match.gameCreation < latest) {
            log.warn("Elo 순서 역전 — $matchId (gameCreation=${match.gameCreation} < 최신=$latest). 전체 재집계로 전환한다.")
            resetAndRecalculate()
            return
        }

        val riotIds = ratableParticipants(match).map { it.riotId }
        val current = eloPort.findAllByRiotIds(riotIds).associateBy { it.riotId }

        val outcome = rate(match, current) ?: return
        eloPort.saveAll(outcome.players)
        eloHistoryPort.saveAll(outcome.histories)
        log.debug("Elo 업데이트 — matchId=${match.matchId}, 대상=${outcome.players.size}명")
    }

    @Transactional
    override fun resetAndRecalculate() {
        log.info("Elo 전체 초기화 시작")
        eloPort.deleteAll()
        eloHistoryPort.deleteAll()

        val matches = matchPersistencePort.findAllOrderedByGameCreation()
        val cache = mutableMapOf<String, PlayerElo>()
        val histories = mutableListOf<PlayerEloHistory>()
        var counted = 0

        for (match in matches) {
            if (skipReason(match) != null) continue
            val outcome = rate(match, cache) ?: continue
            outcome.players.forEach { cache[it.riotId] = it }
            histories += outcome.histories
            counted++
        }

        eloPort.saveAll(cache.values.toList())
        eloHistoryPort.saveAll(histories)
        log.info("Elo 재집계 완료 — 전체 ${matches.size}경기 중 ${counted}경기 반영, ${cache.size}명")
    }

    // ────────── 집계 대상 판별 ──────────

    /** 집계에서 빼야 할 경기면 그 이유를, 정상 경기면 null 을 돌려준다. */
    private fun skipReason(match: Match): String? = when {
        match.queueId == ARAM_QUEUE_ID ->
            "칼바람"
        // 리메이크는 실력이 아니라 누가 안 들어왔느냐의 결과다. 감쇠가 아니라 제외가 맞다.
        match.participants.any { it.gameEndedInEarlySurrender } ->
            "리메이크(조기 종료)"
        match.gameDuration in 1 until REMAKE_DURATION_SEC ->
            "경기 시간 ${match.gameDuration}초 — 리메이크로 간주"
        else -> null
    }

    /**
     * riotId 가 비어 있는 참가자는 제외한다. 그대로 두면 빈 문자열이 한 명의 플레이어로
     * player_elo 에 쌓여 리더보드에 유령이 생긴다. 같은 riotId 가 두 번 들어온 경우도 하나로 접는다.
     */
    private fun ratableParticipants(match: Match): List<MatchParticipant> =
        match.participants.filter { it.riotId.isNotBlank() }.distinctBy { it.riotId }

    // ────────── 한 경기 계산 ──────────

    private class Outcome(val players: List<PlayerElo>, val histories: List<PlayerEloHistory>)

    private fun rate(match: Match, current: Map<String, PlayerElo>): Outcome? {
        val players = ratableParticipants(match)
        val teamA = players.filter { it.teamId == TEAM_A }
        val teamB = players.filter { it.teamId == TEAM_B }
        if (teamA.isEmpty() || teamB.isEmpty()) {
            log.warn("Elo 계산 skip — 한쪽 팀이 비었다: ${match.matchId} (${teamA.size}대${teamB.size})")
            return null
        }

        val aWon = teamA.any { it.win }
        val bWon = teamB.any { it.win }
        if (aWon == bWon) {
            // 양쪽 다 패배(무승부·미완 경기)거나 양쪽 다 승리(데이터 오류). 어느 쪽이든 셀 수 없다.
            log.warn("Elo 계산 skip — 승패를 판정할 수 없다: ${match.matchId}")
            return null
        }

        // 같은 포지션 상대와의 비교 점수. 팀 총량을 팀원끼리 어떻게 나눌지에만 쓰인다.
        val lane = LanePerformance.scores(teamA, teamB)

        fun rated(p: MatchParticipant): EloRating.Rated = EloRating.Rated(
            rating = current[p.riotId]?.elo ?: EloRating.INITIAL,
            games = current[p.riotId]?.games ?: 0,
            lanePerformance = lane[p.riotId] ?: LanePerformance.NEUTRAL,
        )

        val deltas = EloRating.deltas(teamA.map(::rated), teamB.map(::rated), aWon = aWon)

        val now = LocalDateTime.now()
        val updated = mutableListOf<PlayerElo>()
        val histories = mutableListOf<PlayerEloHistory>()

        fun apply(p: MatchParticipant, delta: Double, won: Boolean) {
            val prev = current[p.riotId]
            val before = prev?.elo ?: EloRating.INITIAL
            // 하한을 두지 않는다. 바닥에서 클램프가 걸리면 그만큼이 무에서 생겨나 제로섬이 깨지고,
            // 기록된 delta 와 (eloAfter - eloBefore) 도 어긋난다.
            val after = before + delta
            updated += PlayerElo(
                id = prev?.id ?: 0,
                riotId = p.riotId,
                elo = after,
                games = (prev?.games ?: 0) + 1,
                wins = (prev?.wins ?: 0) + if (won) 1 else 0,
                losses = (prev?.losses ?: 0) + if (won) 0 else 1,
                winStreak = if (won) (prev?.winStreak ?: 0) + 1 else 0,
                lossStreak = if (won) 0 else (prev?.lossStreak ?: 0) + 1,
                updatedAt = now,
            )
            histories += PlayerEloHistory(
                riotId = p.riotId, matchId = match.matchId,
                eloBefore = before, eloAfter = after, delta = delta,
                win = won, lanePerformance = lane[p.riotId] ?: LanePerformance.NEUTRAL,
                gameCreation = match.gameCreation, createdAt = now,
            )
        }

        teamA.forEachIndexed { i, p -> apply(p, deltas.teamA[i], aWon) }
        teamB.forEachIndexed { i, p -> apply(p, deltas.teamB[i], !aWon) }

        return Outcome(updated, histories)
    }
}
