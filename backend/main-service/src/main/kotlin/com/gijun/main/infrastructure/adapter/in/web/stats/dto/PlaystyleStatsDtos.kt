package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.DefeatContributionEntry
import com.gijun.main.application.dto.result.DefeatContributionResult
import com.gijun.main.application.dto.result.PlayerPositionEntry
import com.gijun.main.application.dto.result.PlaystyleDnaEntry
import com.gijun.main.application.dto.result.PlaystyleDnaResult
import com.gijun.main.application.dto.result.PositionBadgeEntry
import com.gijun.main.application.dto.result.PositionBadgeResult
import com.gijun.main.application.dto.result.PositionChampEntry
import com.gijun.main.application.dto.result.PositionChampionPoolResult
import com.gijun.main.application.dto.result.SurvivalIndexEntry
import com.gijun.main.application.dto.result.SurvivalIndexResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "PlaystyleDnaEntry")
data class PlaystyleDnaEntryResponse(
    val riotId: String,
    val games: Int,
    val aggression: Double,
    val durability: Double,
    val teamPlay: Double,
    val objectiveFocus: Double,
    val economy: Double,
    val visionControl: Double,
    val styleTag: String,
) {
    companion object {
        fun from(result: PlaystyleDnaEntry) =
            PlaystyleDnaEntryResponse(
                riotId = result.riotId,
                games = result.games,
                aggression = result.aggression,
                durability = result.durability,
                teamPlay = result.teamPlay,
                objectiveFocus = result.objectiveFocus,
                economy = result.economy,
                visionControl = result.visionControl,
                styleTag = result.styleTag,
            )
    }
}

@Schema(name = "PlaystyleDnaResult")
data class PlaystyleDnaResponse(
    val players: List<PlaystyleDnaEntryResponse>,
) {
    companion object {
        fun from(result: PlaystyleDnaResult) =
            PlaystyleDnaResponse(
                players = result.players.map(PlaystyleDnaEntryResponse::from),
            )
    }
}

@Schema(name = "PositionBadgeEntry")
data class PositionBadgeEntryResponse(
    val position: String,
    val riotId: String,
    val games: Int,
    val winRate: Double,
    val kda: Double,
    val avgDamage: Double,
    val positionScore: Double,
    val topChampion: String?,
    val topChampionId: Int?,
) {
    companion object {
        fun from(result: PositionBadgeEntry) =
            PositionBadgeEntryResponse(
                position = result.position,
                riotId = result.riotId,
                games = result.games,
                winRate = result.winRate,
                kda = result.kda,
                avgDamage = result.avgDamage,
                positionScore = result.positionScore,
                topChampion = result.topChampion,
                topChampionId = result.topChampionId,
            )
    }
}

@Schema(name = "PositionBadgeResult")
data class PositionBadgeResponse(
    val topPositions: List<PositionBadgeEntryResponse>,
    val allPositionRankings: Map<String, List<PositionBadgeEntryResponse>>,
) {
    companion object {
        fun from(result: PositionBadgeResult) =
            PositionBadgeResponse(
                topPositions = result.topPositions.map(PositionBadgeEntryResponse::from),
                allPositionRankings = result.allPositionRankings.mapValues { it.value.map(PositionBadgeEntryResponse::from) },
            )
    }
}

@Schema(name = "PositionChampEntry")
data class PositionChampEntryResponse(
    val champion: String,
    val championId: Int,
    val games: Int,
    val winRate: Double,
    val kda: Double,
) {
    companion object {
        fun from(result: PositionChampEntry) =
            PositionChampEntryResponse(
                champion = result.champion,
                championId = result.championId,
                games = result.games,
                winRate = result.winRate,
                kda = result.kda,
            )
    }
}

@Schema(name = "PlayerPositionEntry")
data class PlayerPositionEntryResponse(
    val riotId: String,
    val position: String,
    val games: Int,
    val winRate: Double,
    val topChampion: String?,
    val topChampionId: Int?,
    val champions: List<PositionChampEntryResponse>,
) {
    companion object {
        fun from(result: PlayerPositionEntry) =
            PlayerPositionEntryResponse(
                riotId = result.riotId,
                position = result.position,
                games = result.games,
                winRate = result.winRate,
                topChampion = result.topChampion,
                topChampionId = result.topChampionId,
                champions = result.champions.map(PositionChampEntryResponse::from),
            )
    }
}

@Schema(name = "PositionChampionPoolResult")
data class PositionChampionPoolResponse(
    val allPlayers: List<PlayerPositionEntryResponse>,
) {
    companion object {
        fun from(result: PositionChampionPoolResult) =
            PositionChampionPoolResponse(
                allPlayers = result.allPlayers.map(PlayerPositionEntryResponse::from),
            )
    }
}

@Schema(name = "SurvivalIndexEntry")
data class SurvivalIndexEntryResponse(
    val riotId: String,
    val games: Int,
    val avgDamageTaken: Double,
    val avgSelfMitigated: Double,
    val avgMitigationRatio: Double,
    val avgTankShare: Double,
    val avgSurvivalRatio: Double,
    val avgDeaths: Double,
    val survivalIndex: Double,
) {
    companion object {
        fun from(result: SurvivalIndexEntry) =
            SurvivalIndexEntryResponse(
                riotId = result.riotId,
                games = result.games,
                avgDamageTaken = result.avgDamageTaken,
                avgSelfMitigated = result.avgSelfMitigated,
                avgMitigationRatio = result.avgMitigationRatio,
                avgTankShare = result.avgTankShare,
                avgSurvivalRatio = result.avgSurvivalRatio,
                avgDeaths = result.avgDeaths,
                survivalIndex = result.survivalIndex,
            )
    }
}

@Schema(name = "SurvivalIndexResult")
data class SurvivalIndexResponse(
    val rankings: List<SurvivalIndexEntryResponse>,
) {
    companion object {
        fun from(result: SurvivalIndexResult) =
            SurvivalIndexResponse(
                rankings = result.rankings.map(SurvivalIndexEntryResponse::from),
            )
    }
}

@Schema(name = "DefeatContributionEntry")
data class DefeatContributionEntryResponse(
    val riotId: String,
    @field:Schema(description = "전체 게임")
    val games: Int,
    @field:Schema(description = "패배 게임")
    val losses: Int,
    val avgDefeatScore: Double,
    val avgDeaths: Double,
    val avgDamage: Double,
    @field:Schema(description = "가장 높은 defeat_score 경기의 matchId")
    val worstMatch: String?,
) {
    companion object {
        fun from(result: DefeatContributionEntry) =
            DefeatContributionEntryResponse(
                riotId = result.riotId,
                games = result.games,
                losses = result.losses,
                avgDefeatScore = result.avgDefeatScore,
                avgDeaths = result.avgDeaths,
                avgDamage = result.avgDamage,
                worstMatch = result.worstMatch,
            )
    }
}

@Schema(name = "DefeatContributionResult")
data class DefeatContributionResponse(
    val rankings: List<DefeatContributionEntryResponse>,
) {
    companion object {
        fun from(result: DefeatContributionResult) =
            DefeatContributionResponse(
                rankings = result.rankings.map(DefeatContributionEntryResponse::from),
            )
    }
}
