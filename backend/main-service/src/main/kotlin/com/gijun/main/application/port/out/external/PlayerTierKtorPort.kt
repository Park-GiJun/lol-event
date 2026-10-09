package com.gijun.main.application.port.out.external

/** 랭크 게임 티어 하나. 마스터 이상은 단계가 없어 [division] 이 늘 `I` 이다. */
data class PlayerTier(
    /** `RANKED_SOLO_5x5` 또는 `RANKED_FLEX_SR`. */
    val queue: String,
    val tier: String,
    val division: String,
    val lp: Int,
)

interface PlayerTierKtorPort {
    /**
     * 지금의 티어. 솔로랭크가 먼저고, 없으면 자유랭크다.
     *
     * @return 배치를 안 봤거나, 그런 Riot ID 가 없거나, Riot API 가 답하지 않으면 null. 예외를 던지지 않는다 —
     *   티어를 모르는 사람은 평균으로 두고 계산을 이어 가면 된다.
     */
    fun findTier(riotId: String): PlayerTier?
}
