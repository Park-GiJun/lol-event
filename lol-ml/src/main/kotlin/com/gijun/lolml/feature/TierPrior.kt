package com.gijun.lolml.feature

import com.gijun.lolml.data.PlayerRank
import com.gijun.lolml.data.QueueRank

/**
 * 내전 밖에서 온 실력 정보: 랭크 게임 티어를 숫자 하나로 바꾼 것.
 *
 * 내전 기록만으로는 처음 온 사람과 몇 판 안 한 사람의 실력을 알 수 없다(전원이 1500 에서 출발한다).
 * 티어는 그 빈자리를 채운다. 다만 **조회한 날의 티어** 라 그 전 경기에는 미래 정보다 — 알고 쓴다.
 *
 * 솔로랭크를 쓰고, 없으면 자유랭크, 둘 다 없으면 모두의 평균이다.
 */
class TierPrior(
    ranks: Map<String, PlayerRank> = emptyMap(),
) {
    private val scores: Map<String, Double> =
        ranks
            .mapNotNull { (playerId, rank) -> (rank.solo ?: rank.flex)?.let { playerId to score(it) } }
            .toMap()
    private val mean = if (scores.isEmpty()) 0.0 else scores.values.average()

    /** 평균적인 사람이 0. 한 티어 위면 +1. 티어를 모르는 사람은 0. */
    fun centered(playerId: String): Double = (scores[playerId] ?: mean) - mean

    companion object {
        /** 낮은 것부터. 마스터 이상은 단계 없이 LP 로만 갈려서 한 줄로 본다. */
        private val TIERS = listOf("IRON", "BRONZE", "SILVER", "GOLD", "PLATINUM", "EMERALD", "DIAMOND")
        private val DIVISIONS = listOf("IV", "III", "II", "I")
        private const val LP_PER_TIER = 400.0

        /**
         * 아이언 IV 0LP = 0, 한 티어에 1. 단계와 LP 는 그 사이를 고르게 나눈다(한 단계 = 100LP = 0.25).
         * 마스터 0LP = 7 이고 그 위는 LP 400 마다 1.
         */
        fun score(rank: QueueRank): Double {
            val tier = TIERS.indexOf(rank.tier)
            return if (tier < 0) {
                TIERS.size + rank.lp / LP_PER_TIER
            } else {
                tier + DIVISIONS.indexOf(rank.division).coerceAtLeast(0) / DIVISIONS.size.toDouble() + rank.lp / LP_PER_TIER
            }
        }
    }
}
