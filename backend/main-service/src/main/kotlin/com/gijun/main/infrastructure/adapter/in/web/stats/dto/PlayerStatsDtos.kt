package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.ChampionCount
import com.gijun.main.application.dto.result.ChampionStat
import com.gijun.main.application.dto.result.GrowthCurveEntry
import com.gijun.main.application.dto.result.GrowthCurveResult
import com.gijun.main.application.dto.result.LaneStat
import com.gijun.main.application.dto.result.PlayerComparisonResult
import com.gijun.main.application.dto.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.result.PlayerStatSnapshot
import com.gijun.main.application.dto.result.PlayerStatsResult
import com.gijun.main.application.dto.result.RecentMatchStat
import com.gijun.main.application.dto.result.StatsResult
import com.gijun.main.application.dto.result.StreakResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "ChampionCount")
data class ChampionCountResponse(
    val champ: String,
    val count: Int,
) {
    companion object {
        fun from(result: ChampionCount) =
            ChampionCountResponse(
                champ = result.champ,
                count = result.count,
            )
    }
}

@Schema(name = "PlayerStatsResult")
data class PlayerStatsResponse(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
    val topChampions: List<ChampionCountResponse>,
) {
    companion object {
        fun from(result: PlayerStatsResult) =
            PlayerStatsResponse(
                riotId = result.riotId,
                games = result.games,
                wins = result.wins,
                losses = result.losses,
                winRate = result.winRate,
                avgKills = result.avgKills,
                avgDeaths = result.avgDeaths,
                avgAssists = result.avgAssists,
                kda = result.kda,
                avgDamage = result.avgDamage,
                avgCs = result.avgCs,
                avgGold = result.avgGold,
                avgVisionScore = result.avgVisionScore,
                topChampions = result.topChampions.map(ChampionCountResponse::from),
            )
    }
}

@Schema(name = "StatsResult")
data class StatsResponse(
    val stats: List<PlayerStatsResponse>,
    val matchCount: Long,
) {
    companion object {
        fun from(result: StatsResult) =
            StatsResponse(
                stats = result.stats.map(PlayerStatsResponse::from),
                matchCount = result.matchCount,
            )
    }
}

@Schema(name = "ChampionStat")
data class ChampionStatResponse(
    val champion: String,
    val championId: Int,
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
) {
    companion object {
        fun from(result: ChampionStat) =
            ChampionStatResponse(
                champion = result.champion,
                championId = result.championId,
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
            )
    }
}

@Schema(name = "RecentMatchStat")
data class RecentMatchStatResponse(
    val matchId: String,
    val champion: String,
    val championId: Int,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val damage: Int,
    val cs: Int,
    val gold: Int,
    val gameCreation: Long,
    val gameDuration: Int,
    val queueId: Int,
) {
    companion object {
        fun from(result: RecentMatchStat) =
            RecentMatchStatResponse(
                matchId = result.matchId,
                champion = result.champion,
                championId = result.championId,
                win = result.win,
                kills = result.kills,
                deaths = result.deaths,
                assists = result.assists,
                damage = result.damage,
                cs = result.cs,
                gold = result.gold,
                gameCreation = result.gameCreation,
                gameDuration = result.gameDuration,
                queueId = result.queueId,
            )
    }
}

@Schema(name = "LaneStat")
data class LaneStatResponse(
    @field:Schema(description = "TOP / JUNGLE / MID / BOTTOM / SUPPORT")
    val position: String,
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
    @field:Schema(description = "탑/서폿 강조")
    val avgDamageTaken: Int,
    @field:Schema(description = "정글 강조")
    val avgObjectiveDamage: Int,
    @field:Schema(description = "서폿 강조")
    val avgWardsPlaced: Double,
    @field:Schema(description = "서폿 강조 (timeCCingOthers)")
    val avgCcTime: Double,
    @field:Schema(description = "정글 강조")
    val avgNeutralMinions: Double,
) {
    companion object {
        fun from(result: LaneStat) =
            LaneStatResponse(
                position = result.position,
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
            )
    }
}

@Schema(name = "PlayerDetailStatsResult")
data class PlayerDetailStatsResponse(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
    val elo: Double,
    val eloRank: Int?,
    val championStats: List<ChampionStatResponse>,
    val recentMatches: List<RecentMatchStatResponse>,
    val laneStats: List<LaneStatResponse>,
) {
    companion object {
        fun from(result: PlayerDetailStatsResult) =
            PlayerDetailStatsResponse(
                riotId = result.riotId,
                games = result.games,
                wins = result.wins,
                losses = result.losses,
                winRate = result.winRate,
                avgKills = result.avgKills,
                avgDeaths = result.avgDeaths,
                avgAssists = result.avgAssists,
                kda = result.kda,
                avgDamage = result.avgDamage,
                avgCs = result.avgCs,
                avgGold = result.avgGold,
                avgVisionScore = result.avgVisionScore,
                elo = result.elo,
                eloRank = result.eloRank,
                championStats = result.championStats.map(ChampionStatResponse::from),
                recentMatches = result.recentMatches.map(RecentMatchStatResponse::from),
                laneStats = result.laneStats.map(LaneStatResponse::from),
            )
    }
}

@Schema(name = "StreakResult")
data class StreakResponse(
    val riotId: String,
    @field:Schema(description = "양수 = 연승, 음수 = 연패, 0 = 경기 없음")
    val currentStreak: Int,
    @field:Schema(description = "\"WIN\", \"LOSS\", \"NONE\"")
    val currentStreakType: String,
    val longestWinStreak: Int,
    val longestLossStreak: Int,
    @field:Schema(description = "최근 10경기 결과: \"W\" / \"L\" (최신순)")
    val recentForm: List<String>,
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
) {
    companion object {
        fun from(result: StreakResult) =
            StreakResponse(
                riotId = result.riotId,
                currentStreak = result.currentStreak,
                currentStreakType = result.currentStreakType,
                longestWinStreak = result.longestWinStreak,
                longestLossStreak = result.longestLossStreak,
                recentForm = result.recentForm,
                totalGames = result.totalGames,
                wins = result.wins,
                losses = result.losses,
            )
    }
}

@Schema(name = "GrowthCurveEntry")
data class GrowthCurveEntryResponse(
    val matchId: String,
    val gameCreation: Long,
    val champion: String,
    val win: Boolean,
    val kda: Double,
    val dmgShare: Double,
    val visionPerMin: Double,
    val csPerMin: Double,
    val rollingKda: Double,
    val rollingDmgShare: Double,
    val rollingCsPerMin: Double,
) {
    companion object {
        fun from(result: GrowthCurveEntry) =
            GrowthCurveEntryResponse(
                matchId = result.matchId,
                gameCreation = result.gameCreation,
                champion = result.champion,
                win = result.win,
                kda = result.kda,
                dmgShare = result.dmgShare,
                visionPerMin = result.visionPerMin,
                csPerMin = result.csPerMin,
                rollingKda = result.rollingKda,
                rollingDmgShare = result.rollingDmgShare,
                rollingCsPerMin = result.rollingCsPerMin,
            )
    }
}

@Schema(name = "GrowthCurveResult")
data class GrowthCurveResponse(
    val riotId: String,
    val entries: List<GrowthCurveEntryResponse>,
    val totalGames: Int,
    val recentAvgKda: Double,
    val overallAvgKda: Double,
    val trend: String,
) {
    companion object {
        fun from(result: GrowthCurveResult) =
            GrowthCurveResponse(
                riotId = result.riotId,
                entries = result.entries.map(GrowthCurveEntryResponse::from),
                totalGames = result.totalGames,
                recentAvgKda = result.recentAvgKda,
                overallAvgKda = result.overallAvgKda,
                trend = result.trend,
            )
    }
}

@Schema(name = "PlayerStatSnapshot")
data class PlayerStatSnapshotResponse(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Double,
    val avgCs: Double,
    val avgGold: Double,
    val avgVisionScore: Double,
) {
    companion object {
        fun from(result: PlayerStatSnapshot) =
            PlayerStatSnapshotResponse(
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
            )
    }
}

@Schema(name = "PlayerComparisonResult")
data class PlayerComparisonResponse(
    val player1: String,
    val player2: String,
    val togetherGames: Int,
    val togetherWinRate: Int,
    val p1TogetherStats: PlayerStatSnapshotResponse?,
    val p2TogetherStats: PlayerStatSnapshotResponse?,
    val versusGames: Int,
    val player1VsWinRate: Int,
    val p1VersusStats: PlayerStatSnapshotResponse?,
    val p2VersusStats: PlayerStatSnapshotResponse?,
    val overallP1Stats: PlayerStatSnapshotResponse,
    val overallP2Stats: PlayerStatSnapshotResponse,
) {
    companion object {
        fun from(result: PlayerComparisonResult) =
            PlayerComparisonResponse(
                player1 = result.player1,
                player2 = result.player2,
                togetherGames = result.togetherGames,
                togetherWinRate = result.togetherWinRate,
                p1TogetherStats = result.p1TogetherStats?.let(PlayerStatSnapshotResponse::from),
                p2TogetherStats = result.p2TogetherStats?.let(PlayerStatSnapshotResponse::from),
                versusGames = result.versusGames,
                player1VsWinRate = result.player1VsWinRate,
                p1VersusStats = result.p1VersusStats?.let(PlayerStatSnapshotResponse::from),
                p2VersusStats = result.p2VersusStats?.let(PlayerStatSnapshotResponse::from),
                overallP1Stats = PlayerStatSnapshotResponse.from(result.overallP1Stats),
                overallP2Stats = PlayerStatSnapshotResponse.from(result.overallP2Stats),
            )
    }
}
