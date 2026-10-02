package com.gijun.main.application.dto.result

data class PlaystyleDnaEntry(
    val riotId: String,
    val games: Int,
    val aggression: Double,
    val durability: Double,
    val teamPlay: Double,
    val objectiveFocus: Double,
    val economy: Double,
    val visionControl: Double,
    val styleTag: String,
)

data class PlaystyleDnaResult(
    val players: List<PlaystyleDnaEntry>,
)

data class PositionBadgeEntry(
    val position: String,
    val riotId: String,
    val games: Int,
    val winRate: Double,
    val kda: Double,
    val avgDamage: Double,
    val positionScore: Double,
    val topChampion: String?,
    val topChampionId: Int?,
)

data class PositionBadgeResult(
    val topPositions: List<PositionBadgeEntry>,
    val allPositionRankings: Map<String, List<PositionBadgeEntry>>,
)

data class PositionChampEntry(
    val champion: String,
    val championId: Int,
    val games: Int,
    val winRate: Double,
    val kda: Double,
)

data class PlayerPositionEntry(
    val riotId: String,
    val position: String,
    val games: Int,
    val winRate: Double,
    val topChampion: String?,
    val topChampionId: Int?,
    val champions: List<PositionChampEntry>,
)

data class PositionChampionPoolResult(
    val allPlayers: List<PlayerPositionEntry>,
)

data class SurvivalIndexEntry(
    val riotId: String,
    val games: Int,
    val avgDamageTaken: Double,
    val avgSelfMitigated: Double,
    val avgMitigationRatio: Double,
    val avgTankShare: Double,
    val avgSurvivalRatio: Double,
    val avgDeaths: Double,
    val survivalIndex: Double,
)

data class SurvivalIndexResult(
    val rankings: List<SurvivalIndexEntry>,
)

data class DefeatContributionEntry(
    val riotId: String,
    val games: Int, // 전체 게임
    val losses: Int, // 패배 게임
    val avgDefeatScore: Double,
    val avgDeaths: Double,
    val avgDamage: Double,
    val worstMatch: String?, // 가장 높은 defeat_score 경기의 matchId
)

data class DefeatContributionResult(
    val rankings: List<DefeatContributionEntry>,
)
