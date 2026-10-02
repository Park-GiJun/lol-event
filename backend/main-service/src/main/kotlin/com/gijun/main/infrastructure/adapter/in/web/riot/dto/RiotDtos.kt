package com.gijun.main.infrastructure.adapter.`in`.web.riot.dto

import com.gijun.main.application.dto.result.RiotMasteryResult
import com.gijun.main.application.dto.result.RiotProfileResult
import com.gijun.main.application.dto.result.RiotRankResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "RiotProfileResult")
data class RiotProfileResponse(
    val riotId: String,
    val puuid: String?,
    val summonerLevel: Long?,
    val profileIconId: Int?,
    val soloRank: RiotRankResponse?,
    val flexRank: RiotRankResponse?,
    val topMastery: List<RiotMasteryResponse>,
) {
    companion object {
        fun from(result: RiotProfileResult) =
            RiotProfileResponse(
                riotId = result.riotId,
                puuid = result.puuid,
                summonerLevel = result.summonerLevel,
                profileIconId = result.profileIconId,
                soloRank = result.soloRank?.let(RiotRankResponse::from),
                flexRank = result.flexRank?.let(RiotRankResponse::from),
                topMastery = result.topMastery.map(RiotMasteryResponse::from),
            )
    }
}

@Schema(name = "RiotRankResult")
data class RiotRankResponse(
    val tier: String,
    val rank: String,
    val lp: Int,
    val wins: Int,
    val losses: Int,
    @field:Schema(description = "0~100.")
    val winRate: Double,
) {
    companion object {
        fun from(result: RiotRankResult) =
            RiotRankResponse(
                tier = result.tier,
                rank = result.rank,
                lp = result.lp,
                wins = result.wins,
                losses = result.losses,
                winRate = result.winRate,
            )
    }
}

@Schema(name = "RiotMasteryResult")
data class RiotMasteryResponse(
    val championId: Int,
    val level: Int,
    val points: Int,
) {
    companion object {
        fun from(result: RiotMasteryResult) =
            RiotMasteryResponse(
                championId = result.championId,
                level = result.level,
                points = result.points,
            )
    }
}
