package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position

/**
 * 개인 성적을 **같은 포지션의 상대와 직접 비교해서** 0~1 로 환산한다. 0.5 가 호각이다.
 *
 * 포지션 보정을 이렇게 푸는 이유가 있다. 서포터의 딜량 점유율은 5%, 원딜은 30%다.
 * 팀 내 점유율을 그대로 쓰면 어떤 가중치를 붙여도 포지션마다 기준선이 달라져,
 * 잘한 서포터가 평범한 원딜보다 낮게 나오는 일이 생긴다.
 * 반면 서포터끼리 비교하면 기준선이 양쪽 모두 0.5 로 같다. 포지션 편향이 구조적으로 사라진다.
 *
 * 그 위에 포지션별 가중치를 한 번 더 얹는다. 서포터는 시야와 킬 관여로, 원딜은 딜량과 CS 로
 * 판단해야 하기 때문이다. 결국 "서포터를 서포터의 잣대로 서포터와 비교"하는 셈이다.
 *
 * 비교 대상을 찾지 못하면([NEUTRAL]) 판단을 보류한다. 포지션 데이터가 깨진 경기에서
 * 엉뚱한 상대와 비교하느니 아무 영향도 주지 않는 쪽이 낫다.
 */
object LanePerformance {

    /** 판단 보류값. 팀 평균과 같아져 배분에 영향을 주지 않는다. */
    const val NEUTRAL = 0.5

    /** riotId -> 라인전 점수(0~1). 양 팀 전원이 들어 있다. */
    fun scores(teamA: List<MatchParticipant>, teamB: List<MatchParticipant>): Map<String, Double> {
        val posA = positionsOf(teamA)
        val posB = positionsOf(teamB)

        // 한 포지션에 두 명이 잡히면 상대를 특정할 수 없다. singleOrNull 로 걸러 보류시킨다.
        val counterpartsForA = teamB.groupBy { posB[it.riotId] ?: Position.UNKNOWN }
        val counterpartsForB = teamA.groupBy { posA[it.riotId] ?: Position.UNKNOWN }

        fun score(
            p: MatchParticipant,
            myPositions: Map<String, Position>,
            counterparts: Map<Position, List<MatchParticipant>>,
        ): Double {
            val pos = myPositions[p.riotId] ?: return NEUTRAL
            if (pos == Position.UNKNOWN) return NEUTRAL
            val opponent = counterparts[pos]?.singleOrNull() ?: return NEUTRAL
            return laneScore(p, opponent, pos)
        }

        return teamA.associate { it.riotId to score(it, posA, counterpartsForA) } +
               teamB.associate { it.riotId to score(it, posB, counterpartsForB) }
    }

    /**
     * 나 대 상대의 지표 우위를 포지션 가중치로 합친 값.
     *
     * 각 지표는 `내 값 / (내 값 + 상대 값)` 이라 자동으로 0~1 에 갇히고, 둘 다 0이면 0.5 가 된다.
     * 가중치는 그 포지션이 실제로 무엇으로 평가받는지를 따른다 — 탑은 CS 와 받아낸 딜,
     * 정글은 오브젝트, 미드·원딜은 딜량, 서포터는 시야와 킬 관여.
     */
    fun laneScore(me: MatchParticipant, opponent: MatchParticipant, pos: Position): Double {
        fun share(mine: Double, theirs: Double): Double {
            val total = mine + theirs
            return if (total <= 0.0) NEUTRAL else mine / total
        }
        fun kda(p: MatchParticipant) = (p.kills + p.assists).toDouble() / maxOf(1, p.deaths)

        val kdaAdv    = share(kda(me), kda(opponent))
        val csAdv     = share(me.cs.toDouble(), opponent.cs.toDouble())
        val goldAdv   = share(me.gold.toDouble(), opponent.gold.toDouble())
        val dmgAdv    = share(me.damage.toDouble(), opponent.damage.toDouble())
        val visionAdv = share(me.visionScore.toDouble(), opponent.visionScore.toDouble())
        val objAdv    = share(me.damageDealtToObjectives.toDouble(), opponent.damageDealtToObjectives.toDouble())
        val tankAdv   = share(me.totalDamageTaken.toDouble(), opponent.totalDamageTaken.toDouble())

        return when (pos) {
            Position.TOP ->
                0.20 * kdaAdv + 0.20 * csAdv + 0.15 * goldAdv + 0.15 * dmgAdv + 0.20 * tankAdv + 0.10 * objAdv
            Position.JUNGLE ->
                0.25 * kdaAdv + 0.10 * csAdv + 0.15 * goldAdv + 0.10 * dmgAdv + 0.30 * objAdv + 0.10 * visionAdv
            Position.MID ->
                0.25 * kdaAdv + 0.20 * csAdv + 0.15 * goldAdv + 0.25 * dmgAdv + 0.10 * visionAdv + 0.05 * objAdv
            Position.ADC ->
                0.20 * kdaAdv + 0.20 * csAdv + 0.20 * goldAdv + 0.30 * dmgAdv + 0.10 * objAdv
            Position.SUPPORT ->
                0.25 * kdaAdv + 0.05 * csAdv + 0.10 * goldAdv + 0.05 * dmgAdv + 0.30 * visionAdv + 0.15 * tankAdv + 0.10 * objAdv
            Position.UNKNOWN ->
                NEUTRAL
        }
    }

    /**
     * 저장된 assignedPosition 이 TOP/JUNGLE/MID/ADC/SUPPORT 를 정확히 하나씩 채우고 있으면 그대로 쓰고,
     * 깨져 있으면 [PositionDetector] 로 추정한다.
     */
    private fun positionsOf(team: List<MatchParticipant>): Map<String, Position> =
        if (PositionDetector.isTeamPositioned(team)) {
            team.associate { p ->
                p.riotId to (runCatching { Position.valueOf(p.assignedPosition.uppercase()) }
                    .getOrDefault(Position.UNKNOWN))
            }
        } else {
            PositionDetector.assignPositions(team)
        }
}
