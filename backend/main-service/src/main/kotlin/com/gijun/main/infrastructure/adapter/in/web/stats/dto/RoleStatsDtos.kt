package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.GoldEfficiencyEntry
import com.gijun.main.application.dto.result.GoldEfficiencyResult
import com.gijun.main.application.dto.result.JungleDominanceEntry
import com.gijun.main.application.dto.result.JungleDominanceResult
import com.gijun.main.application.dto.result.SupportImpactEntry
import com.gijun.main.application.dto.result.SupportImpactResult
import com.gijun.main.application.dto.result.VisionDominanceResult
import com.gijun.main.application.dto.result.VisionPlayerEntry
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "JungleDominanceEntry")
data class JungleDominanceEntryResponse(
    val riotId: String,
    val games: Int,
    val avgInvadeRatio: Double,
    val avgObjShare: Double,
    val avgKp: Double,
    val avgJungleCs: Double,
    val avgJungleDominance: Double,
    val playStyleTag: String,
    val topChampion: String?,
    val topChampionId: Int?,
) {
    companion object {
        fun from(result: JungleDominanceEntry) =
            JungleDominanceEntryResponse(
                riotId = result.riotId,
                games = result.games,
                avgInvadeRatio = result.avgInvadeRatio,
                avgObjShare = result.avgObjShare,
                avgKp = result.avgKp,
                avgJungleCs = result.avgJungleCs,
                avgJungleDominance = result.avgJungleDominance,
                playStyleTag = result.playStyleTag,
                topChampion = result.topChampion,
                topChampionId = result.topChampionId,
            )
    }
}

@Schema(name = "JungleDominanceResult")
data class JungleDominanceResponse(
    val rankings: List<JungleDominanceEntryResponse>,
) {
    companion object {
        fun from(result: JungleDominanceResult) =
            JungleDominanceResponse(
                rankings = result.rankings.map(JungleDominanceEntryResponse::from),
            )
    }
}

@Schema(name = "SupportImpactEntry")
data class SupportImpactEntryResponse(
    val riotId: String,
    val games: Int,
    val avgHealShare: Double,
    val avgCcShare: Double,
    val avgVisionShare: Double,
    val avgShieldProxy: Double,
    val supportImpact: Double,
    val roleTag: String,
    val topChampion: String?,
    val topChampionId: Int?,
) {
    companion object {
        fun from(result: SupportImpactEntry) =
            SupportImpactEntryResponse(
                riotId = result.riotId,
                games = result.games,
                avgHealShare = result.avgHealShare,
                avgCcShare = result.avgCcShare,
                avgVisionShare = result.avgVisionShare,
                avgShieldProxy = result.avgShieldProxy,
                supportImpact = result.supportImpact,
                roleTag = result.roleTag,
                topChampion = result.topChampion,
                topChampionId = result.topChampionId,
            )
    }
}

@Schema(name = "SupportImpactResult")
data class SupportImpactResponse(
    val rankings: List<SupportImpactEntryResponse>,
) {
    companion object {
        fun from(result: SupportImpactResult) =
            SupportImpactResponse(
                rankings = result.rankings.map(SupportImpactEntryResponse::from),
            )
    }
}

@Schema(name = "VisionPlayerEntry")
data class VisionPlayerEntryResponse(
    val riotId: String,
    val games: Int,
    val avgVisionScore: Double,
    val avgWardsPlaced: Double,
    val avgWardsKilled: Double,
    val avgControlWardsBought: Double,
    val wardKillRate: Double,
) {
    companion object {
        fun from(result: VisionPlayerEntry) =
            VisionPlayerEntryResponse(
                riotId = result.riotId,
                games = result.games,
                avgVisionScore = result.avgVisionScore,
                avgWardsPlaced = result.avgWardsPlaced,
                avgWardsKilled = result.avgWardsKilled,
                avgControlWardsBought = result.avgControlWardsBought,
                wardKillRate = result.wardKillRate,
            )
    }
}

@Schema(name = "VisionDominanceResult")
data class VisionDominanceResponse(
    val players: List<VisionPlayerEntryResponse>,
) {
    companion object {
        fun from(result: VisionDominanceResult) =
            VisionDominanceResponse(
                players = result.players.map(VisionPlayerEntryResponse::from),
            )
    }
}

@Schema(name = "GoldEfficiencyEntry")
data class GoldEfficiencyEntryResponse(
    val riotId: String,
    val games: Int,
    val avgDmgPerGold: Double,
    val avgVisionPerGold: Double,
    val avgObjPerGold: Double,
    val avgCsPerGold: Double,
    val goldEfficiencyScore: Double,
    val tags: List<String>,
) {
    companion object {
        fun from(result: GoldEfficiencyEntry) =
            GoldEfficiencyEntryResponse(
                riotId = result.riotId,
                games = result.games,
                avgDmgPerGold = result.avgDmgPerGold,
                avgVisionPerGold = result.avgVisionPerGold,
                avgObjPerGold = result.avgObjPerGold,
                avgCsPerGold = result.avgCsPerGold,
                goldEfficiencyScore = result.goldEfficiencyScore,
                tags = result.tags,
            )
    }
}

@Schema(name = "GoldEfficiencyResult")
data class GoldEfficiencyResponse(
    val rankings: List<GoldEfficiencyEntryResponse>,
    val dmgEfficiencyKing: String?,
    val visionEfficiencyKing: String?,
    val csEfficiencyKing: String?,
    val objEfficiencyKing: String?,
) {
    companion object {
        fun from(result: GoldEfficiencyResult) =
            GoldEfficiencyResponse(
                rankings = result.rankings.map(GoldEfficiencyEntryResponse::from),
                dmgEfficiencyKing = result.dmgEfficiencyKing,
                visionEfficiencyKing = result.visionEfficiencyKing,
                csEfficiencyKing = result.csEfficiencyKing,
                objEfficiencyKing = result.objEfficiencyKing,
            )
    }
}
