package com.gijun.main.application.dto.result

/** Riot 공식 API 에서 가져온 프로필. 등록되지 않은 사람이면 [puuid] 가 null 이고 나머지도 비어 있다. */
data class RiotProfileResult(
    val riotId: String,
    val puuid: String?,
    val summonerLevel: Long?,
    val profileIconId: Int?,
    val soloRank: RiotRankResult?,
    val flexRank: RiotRankResult?,
    val topMastery: List<RiotMasteryResult>,
) {
    companion object {
        /** 내전 멤버로 등록되지 않아 PUUID 를 모르는 사람. */
        fun unregistered(riotId: String) =
            RiotProfileResult(
                riotId = riotId,
                puuid = null,
                summonerLevel = null,
                profileIconId = null,
                soloRank = null,
                flexRank = null,
                topMastery = emptyList(),
            )
    }
}

data class RiotRankResult(
    val tier: String,
    val rank: String,
    val lp: Int,
    val wins: Int,
    val losses: Int,
    /** 0~100. */
    val winRate: Double,
)

data class RiotMasteryResult(
    val championId: Int,
    val level: Int,
    val points: Int,
)
