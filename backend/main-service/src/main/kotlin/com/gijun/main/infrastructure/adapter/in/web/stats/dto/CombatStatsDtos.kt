package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.ChaosMatchEntry
import com.gijun.main.application.dto.result.ChaosMatchResult
import com.gijun.main.application.dto.result.DamageAnalysisResult
import com.gijun.main.application.dto.result.DamagePlayerEntry
import com.gijun.main.application.dto.result.LateGamePlayerEntry
import com.gijun.main.application.dto.result.LateGameResult
import com.gijun.main.application.dto.result.MultiKillEvent
import com.gijun.main.application.dto.result.MultiKillHighlightsResult
import com.gijun.main.application.dto.result.PlayerMultiKillStat
import com.gijun.main.application.dto.result.SurrenderAnalysisResult
import com.gijun.main.application.dto.result.SurrenderPlayerEntry
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "MultiKillEvent")
data class MultiKillEventResponse(
    val riotId: String,
    val champion: String,
    val championId: Int,
    @field:Schema(description = "\"PENTA\", \"QUADRA\", \"TRIPLE\", \"DOUBLE\"")
    val multiKillType: String,
    val matchId: String,
    val gameCreation: Long,
) {
    companion object {
        fun from(result: MultiKillEvent) =
            MultiKillEventResponse(
                riotId = result.riotId,
                champion = result.champion,
                championId = result.championId,
                multiKillType = result.multiKillType,
                matchId = result.matchId,
                gameCreation = result.gameCreation,
            )
    }
}

@Schema(name = "PlayerMultiKillStat")
data class PlayerMultiKillStatResponse(
    val riotId: String,
    val pentaKills: Int,
    val quadraKills: Int,
    val tripleKills: Int,
    val doubleKills: Int,
    val topChampion: String?,
    val topChampionId: Int?,
) {
    companion object {
        fun from(result: PlayerMultiKillStat) =
            PlayerMultiKillStatResponse(
                riotId = result.riotId,
                pentaKills = result.pentaKills,
                quadraKills = result.quadraKills,
                tripleKills = result.tripleKills,
                doubleKills = result.doubleKills,
                topChampion = result.topChampion,
                topChampionId = result.topChampionId,
            )
    }
}

@Schema(name = "MultiKillHighlightsResult")
data class MultiKillHighlightsResponse(
    @field:Schema(description = "전체 펜타킬 이벤트 (최신순)")
    val pentaKillEvents: List<MultiKillEventResponse>,
    @field:Schema(description = "최근 쿼드라 이상 이벤트 20개")
    val recentHighlights: List<MultiKillEventResponse>,
    @field:Schema(description = "플레이어별 멀티킬 합계 (펜타킬 내림차순)")
    val playerRankings: List<PlayerMultiKillStatResponse>,
) {
    companion object {
        fun from(result: MultiKillHighlightsResult) =
            MultiKillHighlightsResponse(
                pentaKillEvents = result.pentaKillEvents.map(MultiKillEventResponse::from),
                recentHighlights = result.recentHighlights.map(MultiKillEventResponse::from),
                playerRankings = result.playerRankings.map(PlayerMultiKillStatResponse::from),
            )
    }
}

@Schema(name = "ChaosMatchEntry")
data class ChaosMatchEntryResponse(
    val matchId: String,
    val gameCreation: Long,
    val gameDurationMin: Double,
    val chaosIndex: Double,
    val totalKills: Int,
    val killDensity: Double,
    val multiKillScore: Int,
    @field:Schema(description = "\"혈전\", \"학살\", \"운영 접전\", \"일반\"")
    val gameTypeTag: String,
    @field:Schema(description = "riotId 목록")
    val participants: List<String>,
) {
    companion object {
        fun from(result: ChaosMatchEntry) =
            ChaosMatchEntryResponse(
                matchId = result.matchId,
                gameCreation = result.gameCreation,
                gameDurationMin = result.gameDurationMin,
                chaosIndex = result.chaosIndex,
                totalKills = result.totalKills,
                killDensity = result.killDensity,
                multiKillScore = result.multiKillScore,
                gameTypeTag = result.gameTypeTag,
                participants = result.participants,
            )
    }
}

@Schema(name = "ChaosMatchResult")
data class ChaosMatchResponse(
    @field:Schema(description = "혼돈 지수 상위 10")
    val topChaosMatches: List<ChaosMatchEntryResponse>,
    @field:Schema(description = "학살 태그 상위 5")
    val topBloodBathMatches: List<ChaosMatchEntryResponse>,
    @field:Schema(description = "운영 접전 상위 5")
    val topStrategicMatches: List<ChaosMatchEntryResponse>,
    val avgChaosIndex: Double,
) {
    companion object {
        fun from(result: ChaosMatchResult) =
            ChaosMatchResponse(
                topChaosMatches = result.topChaosMatches.map(ChaosMatchEntryResponse::from),
                topBloodBathMatches = result.topBloodBathMatches.map(ChaosMatchEntryResponse::from),
                topStrategicMatches = result.topStrategicMatches.map(ChaosMatchEntryResponse::from),
                avgChaosIndex = result.avgChaosIndex,
            )
    }
}

@Schema(name = "DamagePlayerEntry")
data class DamagePlayerEntryResponse(
    val riotId: String,
    val games: Int,
    val avgPhysical: Int,
    val avgMagic: Int,
    val avgTrue: Int,
    val avgTotal: Int,
    val avgMitigated: Int,
    val avgTurretDmg: Int,
    val physicalRate: Double,
    val magicRate: Double,
    val trueRate: Double,
    val damageProfile: String,
) {
    companion object {
        fun from(result: DamagePlayerEntry) =
            DamagePlayerEntryResponse(
                riotId = result.riotId,
                games = result.games,
                avgPhysical = result.avgPhysical,
                avgMagic = result.avgMagic,
                avgTrue = result.avgTrue,
                avgTotal = result.avgTotal,
                avgMitigated = result.avgMitigated,
                avgTurretDmg = result.avgTurretDmg,
                physicalRate = result.physicalRate,
                magicRate = result.magicRate,
                trueRate = result.trueRate,
                damageProfile = result.damageProfile,
            )
    }
}

@Schema(name = "DamageAnalysisResult")
data class DamageAnalysisResponse(
    val players: List<DamagePlayerEntryResponse>,
) {
    companion object {
        fun from(result: DamageAnalysisResult) =
            DamageAnalysisResponse(
                players = result.players.map(DamagePlayerEntryResponse::from),
            )
    }
}

@Schema(name = "SurrenderPlayerEntry")
data class SurrenderPlayerEntryResponse(
    val riotId: String,
    val games: Int,
    val surrenderGames: Int,
    val surrenderRate: Double,
) {
    companion object {
        fun from(result: SurrenderPlayerEntry) =
            SurrenderPlayerEntryResponse(
                riotId = result.riotId,
                games = result.games,
                surrenderGames = result.surrenderGames,
                surrenderRate = result.surrenderRate,
            )
    }
}

@Schema(name = "SurrenderAnalysisResult")
data class SurrenderAnalysisResponse(
    val totalGames: Int,
    val surrenderGames: Int,
    val overallSurrenderRate: Double,
    val players: List<SurrenderPlayerEntryResponse>,
) {
    companion object {
        fun from(result: SurrenderAnalysisResult) =
            SurrenderAnalysisResponse(
                totalGames = result.totalGames,
                surrenderGames = result.surrenderGames,
                overallSurrenderRate = result.overallSurrenderRate,
                players = result.players.map(SurrenderPlayerEntryResponse::from),
            )
    }
}

@Schema(name = "LateGamePlayerEntry")
data class LateGamePlayerEntryResponse(
    val riotId: String,
    val games: Int,
    val avgInhibitorKills: Double,
    val avgChampLevel: Double,
    val avgLongestTimeSpentLiving: Int,
    val avgLargestKillingSpree: Double,
    val avgLargestMultiKill: Double,
    val lateGameScore: Double,
) {
    companion object {
        fun from(result: LateGamePlayerEntry) =
            LateGamePlayerEntryResponse(
                riotId = result.riotId,
                games = result.games,
                avgInhibitorKills = result.avgInhibitorKills,
                avgChampLevel = result.avgChampLevel,
                avgLongestTimeSpentLiving = result.avgLongestTimeSpentLiving,
                avgLargestKillingSpree = result.avgLargestKillingSpree,
                avgLargestMultiKill = result.avgLargestMultiKill,
                lateGameScore = result.lateGameScore,
            )
    }
}

@Schema(name = "LateGameResult")
data class LateGameResponse(
    val players: List<LateGamePlayerEntryResponse>,
) {
    companion object {
        fun from(result: LateGameResult) =
            LateGameResponse(
                players = result.players.map(LateGamePlayerEntryResponse::from),
            )
    }
}
