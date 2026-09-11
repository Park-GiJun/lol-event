package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.rating.LaneResult
import com.gijun.main.domain.model.rating.PlayerRating
import com.gijun.main.domain.model.rating.RatingHistory
import java.time.LocalDateTime

/**
 * 경기 한 판을 두 레이팅에 반영한다. 순수 함수다 — 저장도, 조회도, 로깅도 하지 않는다.
 *
 * 라인 갱신(경기당 5회)과 팀 갱신(경기당 1회)은 **서로 독립**이라 순서가 결과에 영향을 주지 않는다.
 * 그 독립성이 이 설계의 핵심이므로, 한쪽 계산에 다른 쪽 값이 새어 들어가지 않게
 * 두 갱신 모두 **경기 전 값(before)** 만 읽는다.
 */
object RatingEngine {

    const val TEAM_BLUE = 100
    const val TEAM_RED = 200

    /** 재생 대상 최소 경기 시간(초). 이보다 짧으면 리메이크로 본다. */
    const val MIN_DURATION_SEC = 600

    /** 정상 5대5. */
    const val FULL_ROSTER = 10

    class Outcome(
        /** 이 경기에 참여한 사람만. 나머지는 건드리지 않는다. */
        val ratings: List<PlayerRating>,
        val histories: List<RatingHistory>,
        val method: LaneMethod,
        /** 실제로 성립한 라인 맞대결 수. 정상 경기면 5다. */
        val duelCount: Int,
    )

    /**
     * 재생 대상인가. 명세의 필터 그대로다: 10명이 다 있고 600초를 넘긴 경기.
     *
     * 큐로 거르지 않는다는 점에 주의. 칼바람도 팀 레이팅에는 들어간다 — 내전은 내전이다.
     * 다만 라인 맞대결은 [LaneScores.duels] 가 칼바람을 걷어내므로 0개가 되고,
     * 결과적으로 칼바람은 laneElo 를 전혀 건드리지 않는다.
     */
    fun isRatable(match: Match): Boolean =
        match.participants.size == FULL_ROSTER && match.gameDuration > MIN_DURATION_SEC

    /**
     * @param current 정규화된 riotId -> 현재 레이팅. 없는 사람은 기본값에서 시작한다.
     * @param normalize riotId 정규화 함수. [RiotIdNormalizer.canonical] 을 넘긴다.
     * @return 승패를 판정할 수 없거나 한쪽 팀이 비었으면 null
     */
    fun rate(
        match: Match,
        scored: LaneScores.Scored,
        current: Map<String, PlayerRating>,
        normalize: (String) -> String,
        now: LocalDateTime = LocalDateTime.now(),
    ): Outcome? {
        val players = match.participants
            .filter { it.riotId.isNotBlank() }
            .distinctBy { normalize(it.riotId) }

        val blue = players.filter { it.teamId == TEAM_BLUE }
        val red = players.filter { it.teamId == TEAM_RED }
        if (blue.isEmpty() || red.isEmpty()) return null

        val blueWon = blue.any { it.win }
        val redWon = red.any { it.win }
        // 양쪽 다 패배(무승부·미완)거나 양쪽 다 승리(데이터 오류). 어느 쪽이든 셀 수 없다.
        if (blueWon == redWon) return null

        fun ratingOf(riotId: String): PlayerRating {
            val id = normalize(riotId)
            return current[id] ?: PlayerRating(riotId = id)
        }

        val before = players.associate { normalize(it.riotId) to ratingOf(it.riotId) }
        val after = before.toMutableMap()

        // ── 라인 Elo — 경기당 5회 ──
        val opponents = mutableMapOf<String, String>()
        val laneOutcomes = mutableMapOf<String, LaneResult>()
        var duelCount = 0

        for ((pa, pb) in LaneScores.duels(match)) {
            val a = normalize(pa.riotId)
            val b = normalize(pb.riotId)
            if (a == b) continue

            val sa = scored.scores[pa.riotId] ?: continue
            val sb = scored.scores[pb.riotId] ?: continue
            // 동점이면 누가 이겼는지 말할 수 없다. 반반으로 나누지 않고 통째로 건너뛴다 —
            // 무승부 처리는 "정보 없음"을 "호각"으로 바꿔 쓰는 것이라 표본만 늘리고 신호는 없다.
            if (sa == sb) continue

            val ra = before[a] ?: continue
            val rb = before[b] ?: continue

            val expectedA = RatingMath.expected(ra.laneElo, rb.laneElo)
            val aWon = sa > sb
            val scoreA = if (aWon) 1.0 else 0.0
            // 둘 중 표본이 적은 쪽에 맞춰 K 를 잡는다. 쌍 안에서 K 가 같아야 주고받은 점수의 합이 0이 된다.
            val k = RatingMath.kFactor(minOf(ra.laneDuels, rb.laneDuels))

            val deltaA = k * (scoreA - expectedA)

            after[a] = (after[a] ?: ra).copy(
                laneElo = ra.laneElo + deltaA,
                laneDuels = ra.laneDuels + 1,
                laneWins = ra.laneWins + if (aWon) 1 else 0,
            )
            after[b] = (after[b] ?: rb).copy(
                laneElo = rb.laneElo - deltaA,
                laneDuels = rb.laneDuels + 1,
                laneWins = rb.laneWins + if (aWon) 0 else 1,
            )

            opponents[a] = b; opponents[b] = a
            laneOutcomes[a] = if (aWon) LaneResult.WIN else LaneResult.LOSS
            laneOutcomes[b] = if (aWon) LaneResult.LOSS else LaneResult.WIN
            duelCount++
        }

        // ── 팀 Elo — 경기당 1회, 팀 전원 같은 (S - E) ──
        val blueIds = blue.map { normalize(it.riotId) }
        val redIds = red.map { normalize(it.riotId) }
        val blueAvg = blueIds.mapNotNull { before[it]?.teamElo }.average()
        val redAvg = redIds.mapNotNull { before[it]?.teamElo }.average()
        val expectedBlue = RatingMath.expected(blueAvg, redAvg)

        fun applyTeam(ids: List<String>, won: Boolean, expected: Double) {
            val score = if (won) 1.0 else 0.0
            for (id in ids) {
                val r = before[id] ?: continue
                // K 는 개인 표본으로 잡는다. 배치 중인 사람이 섞이면 그 사람만 크게 움직인다.
                val k = RatingMath.kFactor(r.teamGames)
                after[id] = (after[id] ?: r).copy(
                    teamElo = r.teamElo + k * (score - expected),
                    teamGames = r.teamGames + 1,
                    teamWins = r.teamWins + if (won) 1 else 0,
                    // 연승/연패는 화면용 숫자일 뿐이다. 위 식 어디에도 들어가지 않는다.
                    teamWinStreak = if (won) r.teamWinStreak + 1 else 0,
                    teamLossStreak = if (won) 0 else r.teamLossStreak + 1,
                )
            }
        }
        applyTeam(blueIds, blueWon, expectedBlue)
        applyTeam(redIds, !blueWon, 1.0 - expectedBlue)

        val histories = players.map { p ->
            val id = normalize(p.riotId)
            val b = before.getValue(id)
            val a = after.getValue(id)
            RatingHistory(
                riotId = id,
                matchId = match.matchId,
                laneBefore = b.laneElo, laneAfter = a.laneElo,
                laneResult = laneOutcomes[id] ?: LaneResult.NONE,
                laneOpponent = opponents[id],
                teamBefore = b.teamElo, teamAfter = a.teamElo,
                win = p.win,
                gameCreation = match.gameCreation,
                createdAt = now,
            )
        }

        return Outcome(
            ratings = after.values.map { it.copy(updatedAt = now) },
            histories = histories,
            method = scored.method,
            duelCount = duelCount,
        )
    }
}
