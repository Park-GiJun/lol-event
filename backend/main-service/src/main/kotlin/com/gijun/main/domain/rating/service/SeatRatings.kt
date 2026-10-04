package com.gijun.main.domain.rating.service

import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.rating.enums.LaneResult
import com.gijun.main.domain.rating.model.RatingHistoryModel
import kotlin.math.ln

/**
 * 사람 × 포지션별 **자리 Elo** — "이 사람이 이 자리에 앉으면 라인 Elo 가 얼마짜리인가".
 *
 * 라인 Elo 는 사람당 값 하나라 탑에서 번 점수와 원딜에서 번 점수가 섞여 있다. 편성은 사람을 특정
 * 자리에 앉히는 일이므로 그 자리에서의 실적이 따로 필요하다.
 *
 * ## 어떻게 내는가
 * 포지션별 Elo 를 따로 굴리지 않는다. 한 사람의 한 포지션 표본은 대부분 한 자릿수라 독립된 Elo 는
 * 노이즈다. 대신 **전체 라인 Elo 가 예상한 것보다 그 자리에서 얼마나 더 이겼는지**(잔차)를 모은다.
 *
 * ```
 * 잔차      = 실제 결과(1/0) − 경기 전 라인 Elo 로 낸 기대 승률
 * 자리 Elo  = 전체 라인 Elo + 695 × (Σ잔차 + 10 × 기본값) / (그 자리 맞대결 수 + 10)
 * ```
 *
 * - 맞대결이 적으면 기본값 쪽으로 당겨진다. 10 은 [RatingMath.SHRINK_PRIOR] 와 같은 값이다.
 * - 기본값은 주 포지션이면 0, 아니면 **비주력 자리에서 모두가 평균적으로 밑돈 만큼**이다(0 이하).
 *   이게 없으면 미드만 50판 한 사람의 원딜이 미드 실력 그대로 계산된다.
 * - 695 는 승률 50% 근처에서 확률 1 이 Elo 몇 점인지다(400 × 4 / ln10).
 *
 * 개인 지표(KDA·딜량 등)는 여기 들어가지 않는다. [RatingMath] 에 적힌 대로 검증에서 탈락했다.
 * 여기서 쓰는 것은 라인 Elo 를 움직이는 바로 그 맞대결 결과뿐이다.
 *
 * 다 채운 뒤에는 읽기만 한다 — 읽는 쪽은 상태를 바꾸지 않으므로 여러 스레드가 같이 읽어도 된다.
 */
class SeatRatings {
    private class Cell {
        var duels = 0
        var residual = 0.0
    }

    /** riotId → 포지션 → 누적. */
    private val cells = HashMap<String, MutableMap<String, Cell>>()

    /**
     * 경기 한 판의 라인 맞대결을 반영한다.
     *
     * @param histories 그 경기의 레이팅 이력. 기대 승률은 **경기 전** 라인 Elo 로 낸다.
     */
    fun addMatch(
        match: MatchModel,
        histories: List<RatingHistoryModel>,
    ) {
        val byId = histories.associateBy { it.riotId }
        for (history in histories) {
            if (history.laneResult == LaneResult.NONE) continue
            val opponent = history.laneOpponent?.let { byId[it] } ?: continue
            val position = positionOf(match, history.riotId) ?: continue
            add(
                riotId = history.riotId,
                position = position,
                expected = RatingMath.expected(history.laneBefore, opponent.laneBefore),
                won = history.laneResult == LaneResult.WIN,
            )
        }
    }

    fun add(
        riotId: String,
        position: String,
        expected: Double,
        won: Boolean,
    ) {
        val cell = cells.getOrPut(riotId) { HashMap() }.getOrPut(position) { Cell() }
        cell.duels++
        cell.residual += (if (won) 1.0 else 0.0) - expected
    }

    /** 그 자리에서 성립한 라인 맞대결 수. */
    fun duels(
        riotId: String,
        position: String,
    ): Int = cells[riotId]?.get(position)?.duels ?: 0

    /**
     * @param overallElo 그 사람의 전체 라인 Elo 원값.
     * @param shrink 수축의 사전 표본 수. 검증기가 여러 값을 나란히 재 보려고 받는다 — 편성은 기본값을 쓴다.
     * @return 기록이 전혀 없는 사람은 [overallElo] 그대로.
     */
    fun seatElo(
        riotId: String,
        position: String,
        overallElo: Double,
        shrink: Double = SHRINK,
    ): Double {
        val mine = cells[riotId] ?: return overallElo
        val cell = mine[position]
        val prior = if (isMain(mine, position)) 0.0 else offRolePrior()
        val shrunk = ((cell?.residual ?: 0.0) + shrink * prior) / ((cell?.duels ?: 0) + shrink)
        return overallElo + ELO_PER_PROBABILITY * shrunk
    }

    /**
     * 비주력 자리의 평균 잔차. 모든 사람의 비주력 맞대결을 한데 모아 낸다.
     *
     * 양수가 나와도 0 으로 자른다 — 안 가 본 자리가 더 세다고 볼 근거는 없다.
     */
    fun offRolePrior(): Double {
        var duels = 0
        var residual = 0.0
        for (mine in cells.values) {
            for ((position, cell) in mine) {
                if (isMain(mine, position)) continue
                duels += cell.duels
                residual += cell.residual
            }
        }
        return if (duels == 0) 0.0 else (residual / duels).coerceAtMost(0.0)
    }

    /** 맞대결이 가장 많은 자리. 같은 수면 둘 다 주 포지션으로 본다. */
    private fun isMain(
        mine: Map<String, Cell>,
        position: String,
    ): Boolean {
        val here = mine[position]?.duels ?: 0
        return here > 0 && here == mine.values.maxOf { it.duels }
    }

    companion object {
        /** 수축의 사전 표본 수. 그 자리 맞대결이 10번이면 관측과 기본값을 반반 섞는다. */
        const val SHRINK = RatingMath.SHRINK_PRIOR

        /** 기대 승률 50% 근처에서 확률 1 에 해당하는 Elo 점수. */
        val ELO_PER_PROBABILITY = RatingMath.SCALE * 4 / ln(10.0)

        /** 그 경기에서 이 사람이 선 자리. 다섯 자리 중 하나가 아니면 null. */
        fun positionOf(
            match: MatchModel,
            riotId: String,
        ): String? = match.participants.firstOrNull { it.riotId == riotId }?.let { normalize(it.assignedPosition) }

        fun normalize(position: String): String? =
            when (position.uppercase()) {
                "TOP" -> "TOP"
                "JUNGLE" -> "JUNGLE"
                "MID", "MIDDLE" -> "MID"
                "ADC", "BOTTOM" -> "ADC"
                "SUPPORT", "UTILITY" -> "SUPPORT"
                else -> null
            }
    }
}
