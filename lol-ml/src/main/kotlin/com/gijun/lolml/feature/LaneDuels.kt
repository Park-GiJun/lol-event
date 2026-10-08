package com.gijun.lolml.feature

import com.gijun.lolml.data.Match
import com.gijun.lolml.data.Participant
import com.gijun.lolml.data.Position

/** 같은 자리에 선 두 사람의 맞대결 한 번. */
class LaneDuel(
    val winner: Participant,
    val loser: Participant,
)

/**
 * 한 경기의 라인 맞대결. 팀 승패와 달리 **그 사람과 맞상대 둘만의 결과** 다 — 팀 승패는 편성자가
 * 균형을 맞춰 놓아 실력이 잘 안 보이지만, 라인에서 누가 더 컸는지는 그대로 남는다.
 *
 * 같은 자리에 정확히 두 명이 서로 다른 팀으로 있을 때만 성립한다. 점수가 같으면 버린다.
 */
object LaneDuels {
    /** 피해감소 1 = 골드 0.007. 서비스의 `LaneScores.C_MIT` 과 같은 값이다. */
    private const val MITIGATION_WEIGHT = 0.007

    /** 두 사람의 경기 시간이 같아서 분당으로 나누지 않아도 비교 결과는 같다. */
    fun score(participant: Participant): Double = participant.gold + MITIGATION_WEIGHT * participant.damageSelfMitigated

    fun of(match: Match): List<LaneDuel> =
        match.participants
            .filter { it.position != Position.UNKNOWN }
            .groupBy { it.position }
            .values
            .mapNotNull { pair ->
                if (pair.size != 2) return@mapNotNull null
                val (a, b) = pair
                val scoreA = score(a)
                val scoreB = score(b)
                when {
                    a.team == b.team || scoreA == scoreB -> null
                    scoreA > scoreB -> LaneDuel(winner = a, loser = b)
                    else -> LaneDuel(winner = b, loser = a)
                }
            }
}
