package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.BanAnalysisResult
import com.gijun.main.application.dto.result.BanEntry
import com.gijun.main.application.dto.result.ChampionCertEntry
import com.gijun.main.application.dto.result.ChampionCertificateResult
import com.gijun.main.application.dto.result.ChampionDetailStats
import com.gijun.main.application.dto.result.ChampionItemStat
import com.gijun.main.application.dto.result.ChampionLaneStat
import com.gijun.main.application.dto.result.ChampionLaneStrength
import com.gijun.main.application.dto.result.ChampionMatchupResult
import com.gijun.main.application.dto.result.ChampionPlayerStat
import com.gijun.main.application.dto.result.ChampionRuneStat
import com.gijun.main.application.dto.result.ChampionTierEntry
import com.gijun.main.application.dto.result.ChampionTierResult
import com.gijun.main.application.dto.result.LaneGap
import com.gijun.main.application.dto.result.MatchupStat
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "ChampionPlayerStat")
data class ChampionPlayerStatResponse(
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
) {
    companion object {
        fun from(result: ChampionPlayerStat) =
            ChampionPlayerStatResponse(
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

@Schema(name = "ChampionItemStat")
data class ChampionItemStatResponse(
    val itemId: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
) {
    companion object {
        fun from(result: ChampionItemStat) =
            ChampionItemStatResponse(
                itemId = result.itemId,
                picks = result.picks,
                wins = result.wins,
                winRate = result.winRate,
            )
    }
}

@Schema(name = "ChampionRuneStat")
data class ChampionRuneStatResponse(
    @field:Schema(description = "핵심 룬(키스톤) id.")
    val keystone: Int,
    @field:Schema(description = "주 계열 id (정밀 8000 / 지배 8100 / 마법 8200 / 영감 8300 / 결의 8400).")
    val primaryStyle: Int,
    @field:Schema(description = "보조 계열 id.")
    val subStyle: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
) {
    companion object {
        fun from(result: ChampionRuneStat) =
            ChampionRuneStatResponse(
                keystone = result.keystone,
                primaryStyle = result.primaryStyle,
                subStyle = result.subStyle,
                picks = result.picks,
                wins = result.wins,
                winRate = result.winRate,
            )
    }
}

@Schema(name = "ChampionLaneStat")
data class ChampionLaneStatResponse(
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
) {
    companion object {
        fun from(result: ChampionLaneStat) =
            ChampionLaneStatResponse(
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
            )
    }
}

@Schema(name = "ChampionDetailStats")
data class ChampionDetailStatsResponse(
    val champion: String,
    val championId: Int,
    val totalGames: Int,
    val totalWins: Int,
    val winRate: Int,
    val players: List<ChampionPlayerStatResponse>,
    val itemStats: List<ChampionItemStatResponse>,
    @field:Schema(description = "룬 정보가 실려 오지 않은 경기만 있으면 빈 목록이다.")
    val runeStats: List<ChampionRuneStatResponse>,
    val laneStats: List<ChampionLaneStatResponse>,
) {
    companion object {
        fun from(result: ChampionDetailStats) =
            ChampionDetailStatsResponse(
                champion = result.champion,
                championId = result.championId,
                totalGames = result.totalGames,
                totalWins = result.totalWins,
                winRate = result.winRate,
                players = result.players.map(ChampionPlayerStatResponse::from),
                itemStats = result.itemStats.map(ChampionItemStatResponse::from),
                runeStats = result.runeStats.map(ChampionRuneStatResponse::from),
                laneStats = result.laneStats.map(ChampionLaneStatResponse::from),
            )
    }
}

@Schema(name = "LaneGap")
data class LaneGapResponse(
    val goldDiff: Int,
    val csDiff: Double,
    val damageDiff: Int,
    val killDiff: Double,
    val visionDiff: Double,
) {
    companion object {
        fun from(result: LaneGap) =
            LaneGapResponse(
                goldDiff = result.goldDiff,
                csDiff = result.csDiff,
                damageDiff = result.damageDiff,
                killDiff = result.killDiff,
                visionDiff = result.visionDiff,
            )
    }
}

@Schema(name = "ChampionLaneStrength")
data class ChampionLaneStrengthResponse(
    val champion: String,
    val championId: Int,
    val position: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    @field:Schema(description = "표본을 50% 쪽으로 당긴 승률. 정렬은 이 값으로 한다.")
    val adjustedWinRate: Double,
    val sampleGrade: String,
    val gap: LaneGapResponse,
) {
    companion object {
        fun from(result: ChampionLaneStrength) =
            ChampionLaneStrengthResponse(
                champion = result.champion,
                championId = result.championId,
                position = result.position,
                games = result.games,
                wins = result.wins,
                winRate = result.winRate,
                adjustedWinRate = result.adjustedWinRate,
                sampleGrade = result.sampleGrade,
                gap = LaneGapResponse.from(result.gap),
            )
    }
}

@Schema(name = "MatchupStat")
data class MatchupStatResponse(
    val opponent: String,
    val opponentId: Int,
    val position: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val adjustedWinRate: Double,
    val sampleGrade: String,
    val gap: LaneGapResponse,
) {
    companion object {
        fun from(result: MatchupStat) =
            MatchupStatResponse(
                opponent = result.opponent,
                opponentId = result.opponentId,
                position = result.position,
                games = result.games,
                wins = result.wins,
                winRate = result.winRate,
                adjustedWinRate = result.adjustedWinRate,
                sampleGrade = result.sampleGrade,
                gap = LaneGapResponse.from(result.gap),
            )
    }
}

@Schema(name = "ChampionMatchupResult")
data class ChampionMatchupResponse(
    val champion: String,
    val championId: Int,
    @field:Schema(description = "라인별 라인전 지표. 한 챔피언이 여러 라인을 가기도 한다.")
    val laneStrength: List<ChampionLaneStrengthResponse>,
    @field:Schema(description = "유리한 순으로 정렬된 개별 상성.")
    val matchups: List<MatchupStatResponse>,
    @field:Schema(description = "개별 상성에 적용한 최소 표본. 화면에서 \"N경기 이상만\" 이라고 밝히기 위한 값.")
    val minGames: Int,
) {
    companion object {
        fun from(result: ChampionMatchupResult) =
            ChampionMatchupResponse(
                champion = result.champion,
                championId = result.championId,
                laneStrength = result.laneStrength.map(ChampionLaneStrengthResponse::from),
                matchups = result.matchups.map(MatchupStatResponse::from),
                minGames = result.minGames,
            )
    }
}

@Schema(name = "ChampionCertEntry")
data class ChampionCertEntryResponse(
    val riotId: String,
    val champion: String,
    val championId: Int,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Double,
    @field:Schema(description = "표본을 50% 쪽으로 당긴 승률. 정렬은 반드시 이 값으로 한다.")
    val adjustedWinRate: Double,
    @field:Schema(description = "HIGH / MEDIUM / LOW / INSUFFICIENT")
    val sampleGrade: String,
    val certified: Boolean,
) {
    companion object {
        fun from(result: ChampionCertEntry) =
            ChampionCertEntryResponse(
                riotId = result.riotId,
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
                adjustedWinRate = result.adjustedWinRate,
                sampleGrade = result.sampleGrade,
                certified = result.certified,
            )
    }
}

@Schema(name = "ChampionCertificateResult")
data class ChampionCertificateResponse(
    val certifiedMasters: List<ChampionCertEntryResponse>,
    val topChampionMasters: Map<String, ChampionCertEntryResponse>,
) {
    companion object {
        fun from(result: ChampionCertificateResult) =
            ChampionCertificateResponse(
                certifiedMasters = result.certifiedMasters.map(ChampionCertEntryResponse::from),
                topChampionMasters = result.topChampionMasters.mapValues { ChampionCertEntryResponse.from(it.value) },
            )
    }
}

@Schema(name = "ChampionTierEntry")
data class ChampionTierEntryResponse(
    val champion: String,
    val championId: Int,
    val tier: String,
    val tierScore: Double,
    val games: Int,
    @field:Schema(description = "관측 승률. 표본이 작으면 그대로 믿으면 안 된다. 항상 games 와 같이 보여줄 것.")
    val winRate: Int,
    @field:Schema(description = "전체 평균 쪽으로 끌어당긴 승률. 정렬은 이 값으로 한다.")
    val adjustedWinRate: Double,
    @field:Schema(description = "HIGH / MEDIUM / LOW / INSUFFICIENT")
    val sampleGrade: String,
    val kda: Double,
    val pickRate: Double,
    val avgDamage: Double,
) {
    companion object {
        fun from(result: ChampionTierEntry) =
            ChampionTierEntryResponse(
                champion = result.champion,
                championId = result.championId,
                tier = result.tier,
                tierScore = result.tierScore,
                games = result.games,
                winRate = result.winRate,
                adjustedWinRate = result.adjustedWinRate,
                sampleGrade = result.sampleGrade,
                kda = result.kda,
                pickRate = result.pickRate,
                avgDamage = result.avgDamage,
            )
    }
}

@Schema(name = "ChampionTierResult")
data class ChampionTierResponse(
    val tierList: List<ChampionTierEntryResponse>,
    val byTier: Map<String, List<ChampionTierEntryResponse>>,
    val totalMatches: Int,
) {
    companion object {
        fun from(result: ChampionTierResult) =
            ChampionTierResponse(
                tierList = result.tierList.map(ChampionTierEntryResponse::from),
                byTier = result.byTier.mapValues { it.value.map(ChampionTierEntryResponse::from) },
                totalMatches = result.totalMatches,
            )
    }
}

@Schema(name = "BanEntry")
data class BanEntryResponse(
    val champion: String,
    val championId: Int,
    val banCount: Int,
    @field:Schema(description = "banCount / totalGames")
    val banRate: Double,
) {
    companion object {
        fun from(result: BanEntry) =
            BanEntryResponse(
                champion = result.champion,
                championId = result.championId,
                banCount = result.banCount,
                banRate = result.banRate,
            )
    }
}

@Schema(name = "BanAnalysisResult")
data class BanAnalysisResponse(
    val topBanned: List<BanEntryResponse>,
    val totalGamesAnalyzed: Int,
    val mostBannedChampion: String?,
) {
    companion object {
        fun from(result: BanAnalysisResult) =
            BanAnalysisResponse(
                topBanned = result.topBanned.map(BanEntryResponse::from),
                totalGamesAnalyzed = result.totalGamesAnalyzed,
                mostBannedChampion = result.mostBannedChampion,
            )
    }
}
