package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.DuoStat
import com.gijun.main.application.dto.result.DuoStatsResult
import com.gijun.main.application.dto.result.RivalMatchupEntry
import com.gijun.main.application.dto.result.RivalMatchupResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "DuoStat")
data class DuoStatResponse(
    val player1: String,
    val player2: String,
    val games: Int,
    val wins: Int,
    @field:Schema(description = "관측 승률. 항상 games 와 같이 보여줄 것.")
    val winRate: Int,
    @field:Schema(description = "전체 평균 쪽으로 끌어당긴 승률. 정렬은 이 값으로 한다.")
    val adjustedWinRate: Double,
    @field:Schema(description = "HIGH / MEDIUM / LOW / INSUFFICIENT")
    val sampleGrade: String,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
) {
    companion object {
        fun from(result: DuoStat) =
            DuoStatResponse(
                player1 = result.player1,
                player2 = result.player2,
                games = result.games,
                wins = result.wins,
                winRate = result.winRate,
                adjustedWinRate = result.adjustedWinRate,
                sampleGrade = result.sampleGrade,
                avgKills = result.avgKills,
                avgDeaths = result.avgDeaths,
                avgAssists = result.avgAssists,
                kda = result.kda,
            )
    }
}

@Schema(name = "DuoStatsResult")
data class DuoStatsResponse(
    val duos: List<DuoStatResponse>,
) {
    companion object {
        fun from(result: DuoStatsResult) =
            DuoStatsResponse(
                duos = result.duos.map(DuoStatResponse::from),
            )
    }
}

@Schema(name = "RivalMatchupEntry")
data class RivalMatchupEntryResponse(
    val player1: String,
    val player2: String,
    val games: Int,
    val player1Wins: Int,
    val player2Wins: Int,
    val player1WinRate: Int,
    @field:Schema(description = "표본을 50% 쪽으로 당긴 player1 승률. 정렬은 이 값으로 한다.")
    val player1AdjustedWinRate: Double,
    @field:Schema(description = "HIGH / MEDIUM / LOW / INSUFFICIENT")
    val sampleGrade: String,
) {
    companion object {
        fun from(result: RivalMatchupEntry) =
            RivalMatchupEntryResponse(
                player1 = result.player1,
                player2 = result.player2,
                games = result.games,
                player1Wins = result.player1Wins,
                player2Wins = result.player2Wins,
                player1WinRate = result.player1WinRate,
                player1AdjustedWinRate = result.player1AdjustedWinRate,
                sampleGrade = result.sampleGrade,
            )
    }
}

@Schema(name = "RivalMatchupResult")
data class RivalMatchupResponse(
    val rivalries: List<RivalMatchupEntryResponse>,
    val topRivalry: RivalMatchupEntryResponse?,
) {
    companion object {
        fun from(result: RivalMatchupResult) =
            RivalMatchupResponse(
                rivalries = result.rivalries.map(RivalMatchupEntryResponse::from),
                topRivalry = result.topRivalry?.let(RivalMatchupEntryResponse::from),
            )
    }
}
