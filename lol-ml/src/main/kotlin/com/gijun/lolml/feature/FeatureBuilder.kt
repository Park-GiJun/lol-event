package com.gijun.lolml.feature

import com.gijun.lolml.data.Match
import com.gijun.lolml.data.Participant
import com.gijun.lolml.data.Position
import com.gijun.lolml.data.Team
import kotlin.math.ln
import kotlin.math.min

/** 학습 표본 하나. [features] 의 순서는 [FeatureBuilder.NAMES] 와 같다. */
class Example(
    val matchId: String,
    val features: DoubleArray,
    /** 블루 승 = 1.0 */
    val label: Double,
    /** 팀 구성. 피처를 거치지 않고 "누가 있었나" 를 직접 배우는 모델이 쓴다. */
    val blue: List<String> = emptyList(),
    val red: List<String> = emptyList(),
)

/**
 * 경기를 시간순으로 재생하며 피처를 만든다.
 *
 * 한 경기에서 순서는 반드시: **지금 상태로 피처 생성 → 그 다음 이 경기 결과로 상태 갱신.**
 * 뒤집히면 그 경기의 결과가 그 경기의 피처에 샌다.
 */
class FeatureBuilder {
    private val states = HashMap<String, PlayerState>()

    /** [matches] 는 시간순이어야 한다. */
    fun build(matches: List<Match>): List<Example> =
        matches.map { match ->
            val blue = match.participants.filter { it.team == Team.BLUE }
            val red = match.participants.filter { it.team == Team.RED }

            // 1) 경기 전 상태로만 피처를 만든다. 여기서는 win·kills·gold 같은 결과를 읽지 않는다.
            //    자리(position)는 경기 전에 정해지므로 읽어도 된다.
            val blueElo = mean(blue) { state, _ -> state.elo }
            val redElo = mean(red) { state, _ -> state.elo }
            val features =
                doubleArrayOf(
                    blueElo - redElo,
                    diff(blue, red) { state, _ -> state.recentKda() },
                    diff(blue, red) { state, _ -> ln(1.0 + state.games) },
                    diff(blue, red) { state, position -> state.seatShare(position) },
                    diff(blue, red) { state, position -> state.seatEdge(position) },
                    diff(blue, red) { state, _ -> state.laneElo },
                    diff(blue, red) { state, _ -> state.winRate() },
                    diff(blue, red) { state, position -> state.seatWinRate(position) },
                    diff(blue, red) { state, _ -> state.laneWinRate() },
                    diff(blue, red) { state, position -> state.seatLaneWinRate(position) },
                    offRolePrior().let { prior -> diff(blue, red) { state, position -> state.seatElo(position, prior) } },
                )
            val example =
                Example(
                    matchId = match.matchId,
                    features = features,
                    label = if (match.blueWin) 1.0 else 0.0,
                    blue = blue.map { it.playerId },
                    red = red.map { it.playerId },
                )

            // 2) 피처를 다 뽑은 뒤에야 결과를 반영한다.
            val blueExpected = Elo.expected(blueElo, redElo)
            blue.forEach { state(it).update(it, blueExpected) }
            red.forEach { state(it).update(it, 1.0 - blueExpected) }
            LaneDuels.of(match).forEach(::applyDuel)

            example
        }

    /** 맞대결 한 번. 두 사람 모두 **맞대결 전** 라인 Elo 로 기대 승률을 낸 뒤에 같이 움직인다. */
    private fun applyDuel(duel: LaneDuel) {
        val winner = state(duel.winner)
        val loser = state(duel.loser)
        val expected = Elo.expected(winner.laneElo, loser.laneElo)
        // 서비스의 레이팅과 같은 K 다. 둘 중 표본이 적은 쪽에 맞춘다.
        val k = if (min(winner.laneDuels, loser.laneDuels) < LANE_PLACEMENT) LANE_K_PLACEMENT else LANE_K
        val delta = k * (1.0 - expected)
        winner.updateLane(duel.winner.position, won = true, expected = expected, newLaneElo = winner.laneElo + delta)
        loser.updateLane(duel.loser.position, won = false, expected = 1.0 - expected, newLaneElo = loser.laneElo - delta)
    }

    /** 비주력 자리의 평균 잔차. 양수가 나와도 0 으로 자른다 — 안 가 본 자리가 더 세다고 볼 근거는 없다. */
    private fun offRolePrior(): Double {
        var duels = 0
        var residual = 0.0
        for (state in states.values) {
            val (d, r) = state.offRoleTotals()
            duels += d
            residual += r
        }
        return if (duels == 0) 0.0 else (residual / duels).coerceAtMost(0.0)
    }

    private fun state(participant: Participant): PlayerState = states.getOrPut(participant.playerId) { PlayerState() }

    /** 사람마다 값을 뽑아 팀 평균을 낸다. 자리별 값은 **이번 경기에서 앉은 자리** 로 뽑는다. */
    private fun mean(
        team: List<Participant>,
        value: (PlayerState, Position) -> Double,
    ): Double = team.map { value(state(it), it.position) }.average()

    private fun diff(
        blue: List<Participant>,
        red: List<Participant>,
        value: (PlayerState, Position) -> Double,
    ): Double = mean(blue, value) - mean(red, value)

    companion object {
        private const val LANE_K = 16.0
        private const val LANE_K_PLACEMENT = 26.0
        private const val LANE_PLACEMENT = 10

        /** 전부 (블루 팀 평균 − 레드 팀 평균). */
        val NAMES =
            listOf(
                "eloDiff",
                "recentKdaDiff",
                "logGamesDiff",
                "seatShareDiff",
                "seatEdgeDiff",
                "laneEloDiff",
                "winRateDiff",
                "seatWinRateDiff",
                "laneWinRateDiff",
                "seatLaneWinRateDiff",
                "seatEloDiff",
            )
    }
}
