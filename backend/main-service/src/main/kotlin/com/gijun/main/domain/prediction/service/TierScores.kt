package com.gijun.main.domain.prediction.service

/**
 * 랭크 게임 티어를 숫자 하나로. `lol-ml` 의 `TierPrior.score` 와 같은 공식이어야 한다 —
 * 모델의 티어 계수가 이 눈금에 맞춰 학습됐다.
 *
 * 아이언 IV 0LP = 0, 한 티어에 1. 단계와 LP 는 그 사이를 고르게 나눈다(한 단계 = 100LP = 0.25).
 * 마스터 이상은 단계 없이 LP 로만 갈려서 한 줄로 본다: 마스터 0LP = 7 이고 LP 400 마다 1.
 */
object TierScores {
    private val TIERS = listOf("IRON", "BRONZE", "SILVER", "GOLD", "PLATINUM", "EMERALD", "DIAMOND")
    private val DIVISIONS = listOf("IV", "III", "II", "I")
    private const val LP_PER_TIER = 400.0

    fun score(
        tier: String,
        division: String,
        lp: Int,
    ): Double {
        val index = TIERS.indexOf(tier.uppercase())
        return if (index < 0) {
            TIERS.size + lp / LP_PER_TIER
        } else {
            index + DIVISIONS.indexOf(division.uppercase()).coerceAtLeast(0) / DIVISIONS.size.toDouble() + lp / LP_PER_TIER
        }
    }

    /** 점수를 다시 사람이 읽는 말로. 팀 평균처럼 실제 티어가 아닌 값을 보여 줄 때 쓴다. 예: 4.6 → `PLATINUM II`. */
    fun label(score: Double): String {
        if (score >= TIERS.size) return "MASTER+"
        val clamped = score.coerceAtLeast(0.0)
        val tier = clamped.toInt()
        val division = ((clamped - tier) * DIVISIONS.size).toInt().coerceAtMost(DIVISIONS.lastIndex)
        return "${TIERS[tier]} ${DIVISIONS[division]}"
    }
}
