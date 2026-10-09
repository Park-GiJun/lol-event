package com.gijun.lolml.feature

import com.gijun.lolml.data.Participant
import com.gijun.lolml.data.Position
import kotlin.math.ln
import kotlin.math.max

/** 한 사람의 "지금까지" 상태. 경기가 끝날 때마다 [FeatureBuilder] 가 갱신한다. */
class PlayerState(
    initialTierLaneElo: Double = Elo.INITIAL,
) {
    var elo: Double = Elo.INITIAL
        private set
    var games: Int = 0
        private set
    private var wins = 0

    /** 라인 맞대결로만 움직이는 Elo. 비교용이다 — Elo 없이 센 승률 피처와 나란히 놓고 본다. */
    var laneElo: Double = Elo.INITIAL
        private set
    var laneDuels: Int = 0
        private set
    private var laneWins = 0

    /** 라인 Elo 와 같은 맞대결로 움직이되, 1500 이 아니라 랭크 게임 티어에서 출발한다([TierPrior]). */
    var tierLaneElo: Double = initialTierLaneElo
        private set

    private val recentKdas = ArrayDeque<Double>()
    private val seats = HashMap<Position, Seat>()

    /** 최근 N 경기 KDA 평균. 경기가 없으면 기본값. */
    fun recentKda(): Double = if (recentKdas.isEmpty()) DEFAULT_KDA else recentKdas.average()

    /** 지금까지 한 경기 중 이 자리에서 한 비율. 주 포지션이면 1 에 가깝고 처음 앉는 자리면 0. */
    fun seatShare(position: Position): Double {
        val seat = seats[position] ?: return 0.0
        return seat.games.toDouble() / games
    }

    /**
     * 이 자리에 앉았을 때 Elo 가 기대한 것보다 얼마나 더 이겼는가(경기당).
     * 표본이 적으면 0 쪽으로 당긴다.
     */
    fun seatEdge(position: Position): Double {
        val seat = seats[position] ?: return 0.0
        return seat.surprise / (seat.games + PRIOR)
    }

    /*
     * 아래 넷은 Elo 를 쓰지 않고 이긴 횟수만 센다.
     *
     * 전부 (이긴 횟수 + PRIOR × 기본값) / (횟수 + PRIOR) 꼴이다. 기록이 없으면 기본값 그대로이고,
     * 쌓일수록 실제 비율에 가까워진다. 10전 8승을 80% 로 믿지 않으려는 장치다.
     * 자리별 값의 기본값은 그 사람의 전체 값이다 — 처음 앉는 자리는 "평소 실력만큼 한다" 에서 출발한다.
     */

    /** 팀 승률. 기본값 0.5. */
    fun winRate(): Double = shrunk(wins, games, HALF)

    /** 이 자리에 앉았을 때의 팀 승률. */
    fun seatWinRate(position: Position): Double = shrunk(seats[position]?.wins ?: 0, seats[position]?.games ?: 0, winRate())

    /** 라인 맞대결 승률. 기본값 0.5. */
    fun laneWinRate(): Double = shrunk(laneWins, laneDuels, HALF)

    /** 이 자리에서의 라인 맞대결 승률. */
    fun seatLaneWinRate(position: Position): Double = shrunk(seats[position]?.laneWins ?: 0, seats[position]?.laneDuels ?: 0, laneWinRate())

    /**
     * 서비스의 팀 편성이 쓰는 **자리 Elo** 를 로우 데이터로 다시 계산한 것(`SeatRatings.seatElo`).
     *
     * 라인 Elo 에, 그 자리에서 라인 Elo 가 기대한 것보다 더 이긴 정도(잔차)를 점수로 바꿔 얹는다.
     * 맞대결이 [SEAT_ELO_SHRINK] 번은 되어야 관측을 절반 반영한다. 주 포지션이 아닌 자리는
     * [offRolePrior] (모두가 비주력 자리에서 평균적으로 밑돈 만큼, 0 이하) 쪽으로 당긴다.
     */
    fun seatElo(
        position: Position,
        offRolePrior: Double,
    ): Double {
        if (laneDuels == 0 || position == Position.UNKNOWN) return laneElo
        val seat = seats[position]
        val prior = if (isMainSeat(position)) 0.0 else offRolePrior
        val shrunk = ((seat?.laneResidual ?: 0.0) + SEAT_ELO_SHRINK * prior) / ((seat?.laneDuels ?: 0) + SEAT_ELO_SHRINK)
        return laneElo + ELO_PER_PROBABILITY * shrunk
    }

    /** 주 포지션이 아닌 자리들의 (맞대결 수, 잔차 합). 전원의 것을 모아 비주력 기본값을 낸다. */
    fun offRoleTotals(): Pair<Int, Double> {
        var duels = 0
        var residual = 0.0
        for ((position, seat) in seats) {
            if (isMainSeat(position)) continue
            duels += seat.laneDuels
            residual += seat.laneResidual
        }
        return duels to residual
    }

    /** 라인 맞대결이 가장 많은 자리. 같은 수면 둘 다 주 포지션으로 본다. */
    private fun isMainSeat(position: Position): Boolean {
        val here = seats[position]?.laneDuels ?: 0
        return here > 0 && here == seats.values.maxOf { it.laneDuels }
    }

    /** 이번 경기 결과를 반영한다. 피처를 뽑은 **다음에만** 부른다. */
    fun update(
        participant: Participant,
        teamExpected: Double,
    ) {
        val score = if (participant.win) 1.0 else 0.0
        // 팀 게임이라 기대 승률은 팀 것을 쓴다. 같은 팀 다섯은 같은 만큼 오르내린다.
        elo = Elo.updated(elo, teamExpected, score)
        games += 1
        if (participant.win) wins += 1

        // 데스가 0 이면 1 로 나눈다(나눗셈이 터지지 않게).
        recentKdas.addLast((participant.kills + participant.assists).toDouble() / max(1, participant.deaths))
        if (recentKdas.size > RECENT_WINDOW) recentKdas.removeFirst()

        // 자리를 모르는 경기는 어느 자리의 기록으로도 치지 않는다.
        if (participant.position != Position.UNKNOWN) {
            val seat = seats.getOrPut(participant.position) { Seat() }
            seat.games += 1
            if (participant.win) seat.wins += 1
            seat.surprise += score - teamExpected
        }
    }

    /** 이번 경기의 라인 맞대결 결과를 반영한다. 이것도 피처를 뽑은 **다음에만** 부른다. */
    fun updateLane(
        position: Position,
        won: Boolean,
        /** 맞대결 전 라인 Elo 로 낸, 이 사람이 이길 기대 확률. */
        expected: Double,
        newLaneElo: Double,
        /** 따로 주지 않으면 라인 Elo 와 같이 움직인다. */
        newTierLaneElo: Double = newLaneElo,
    ) {
        laneElo = newLaneElo
        tierLaneElo = newTierLaneElo
        laneDuels += 1
        val seat = seats.getOrPut(position) { Seat() }
        seat.laneDuels += 1
        seat.laneResidual += (if (won) 1.0 else 0.0) - expected
        if (won) {
            laneWins += 1
            seat.laneWins += 1
        }
    }

    private fun shrunk(
        wins: Int,
        count: Int,
        prior: Double,
    ): Double = (wins + PRIOR * prior) / (count + PRIOR)

    private class Seat {
        var games = 0
        var wins = 0

        /** (결과 − 기대 승률) 의 합. 기대보다 많이 이겼으면 양수. */
        var surprise = 0.0
        var laneDuels = 0
        var laneWins = 0

        /** (라인 결과 − 라인 Elo 의 기대 승률) 의 합. */
        var laneResidual = 0.0
    }

    companion object {
        const val RECENT_WINDOW = 10

        /** 기록이 없는 사람에게 주는 값. 평범한 KDA 근처로 둔다. */
        const val DEFAULT_KDA = 3.0

        /** 기본값 쪽으로 당기는 세기. 이 횟수만큼 "기본값대로였다" 는 가짜 기록을 깔아 두는 셈이다. */
        const val PRIOR = 10.0

        /** 서비스의 `SeatRatings.SHRINK` 와 같은 값이다. */
        const val SEAT_ELO_SHRINK = 80.0

        /** 승률 50% 근처에서 확률 1 이 Elo 몇 점인지(400 × 4 / ln 10 ≈ 695). */
        val ELO_PER_PROBABILITY = 400.0 * 4 / ln(10.0)

        private const val HALF = 0.5
    }
}
