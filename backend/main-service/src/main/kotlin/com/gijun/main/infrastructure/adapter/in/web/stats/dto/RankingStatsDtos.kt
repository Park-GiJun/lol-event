package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.KillParticipationEntry
import com.gijun.main.application.dto.result.KillParticipationResult
import com.gijun.main.application.dto.result.LaneLeaderboardResult
import com.gijun.main.application.dto.result.MvpPlayerStat
import com.gijun.main.application.dto.result.MvpStatsResult
import com.gijun.main.application.dto.result.PlayerLaneStat
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "PlayerLaneStat")
data class PlayerLaneStatResponse(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
    val avgDamageTaken: Int,
    val avgObjectiveDamage: Int,
    val avgWardsPlaced: Double,
    val avgCcTime: Double,
    val avgNeutralMinions: Double,
    val topChampion: String?,
    val topChampionId: Int?,
) {
    companion object {
        fun from(result: PlayerLaneStat) =
            PlayerLaneStatResponse(
                riotId = result.riotId,
                games = result.games,
                wins = result.wins,
                winRate = result.winRate,
                avgKills = result.avgKills,
                avgDeaths = result.avgDeaths,
                avgAssists = result.avgAssists,
                kda = result.kda,
                avgDamage = result.avgDamage,
                avgCs = result.avgCs,
                avgGold = result.avgGold,
                avgVisionScore = result.avgVisionScore,
                avgDamageTaken = result.avgDamageTaken,
                avgObjectiveDamage = result.avgObjectiveDamage,
                avgWardsPlaced = result.avgWardsPlaced,
                avgCcTime = result.avgCcTime,
                avgNeutralMinions = result.avgNeutralMinions,
                topChampion = result.topChampion,
                topChampionId = result.topChampionId,
            )
    }
}

@Schema(name = "LaneLeaderboardResult")
data class LaneLeaderboardResponse(
    val lane: String,
    val players: List<PlayerLaneStatResponse>,
) {
    companion object {
        fun from(result: LaneLeaderboardResult) =
            LaneLeaderboardResponse(
                lane = result.lane,
                players = result.players.map(PlayerLaneStatResponse::from),
            )
    }
}

@Schema(name = "MvpPlayerStat")
data class MvpPlayerStatResponse(
    val riotId: String,
    val games: Int,
    @field:Schema(description = "팀 내 최고 점수 횟수")
    val mvpCount: Int,
    @field:Schema(description = "경기 전체 최고 점수 횟수")
    val aceCount: Int,
    @field:Schema(description = "MVP 달성률 (%)")
    val mvpRate: Int,
    @field:Schema(description = "평균 MVP 점수")
    val avgMvpScore: Double,
    @field:Schema(description = "MVP 달성 최다 챔피언")
    val topChampion: String?,
    val topChampionId: Int?,
) {
    companion object {
        fun from(result: MvpPlayerStat) =
            MvpPlayerStatResponse(
                riotId = result.riotId,
                games = result.games,
                mvpCount = result.mvpCount,
                aceCount = result.aceCount,
                mvpRate = result.mvpRate,
                avgMvpScore = result.avgMvpScore,
                topChampion = result.topChampion,
                topChampionId = result.topChampionId,
            )
    }
}

@Schema(name = "MvpStatsResult")
data class MvpStatsResponse(
    val rankings: List<MvpPlayerStatResponse>,
    val totalGames: Int,
) {
    companion object {
        fun from(result: MvpStatsResult) =
            MvpStatsResponse(
                rankings = result.rankings.map(MvpPlayerStatResponse::from),
                totalGames = result.totalGames,
            )
    }
}

@Schema(name = "KillParticipationEntry")
data class KillParticipationEntryResponse(
    val riotId: String,
    val games: Int,
    @field:Schema(description = "(kills+assists) / teamKills")
    val avgKp: Double,
    val avgKpWin: Double,
    val avgKpLoss: Double,
    val avgKills: Double,
    val avgAssists: Double,
) {
    companion object {
        fun from(result: KillParticipationEntry) =
            KillParticipationEntryResponse(
                riotId = result.riotId,
                games = result.games,
                avgKp = result.avgKp,
                avgKpWin = result.avgKpWin,
                avgKpLoss = result.avgKpLoss,
                avgKills = result.avgKills,
                avgAssists = result.avgAssists,
            )
    }
}

@Schema(name = "KillParticipationResult")
data class KillParticipationResponse(
    val rankings: List<KillParticipationEntryResponse>,
    val kpKing: String?,
) {
    companion object {
        fun from(result: KillParticipationResult) =
            KillParticipationResponse(
                rankings = result.rankings.map(KillParticipationEntryResponse::from),
                kpKing = result.kpKing,
            )
    }
}
