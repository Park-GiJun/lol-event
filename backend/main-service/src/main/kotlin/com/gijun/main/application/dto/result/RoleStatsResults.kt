package com.gijun.main.application.dto.result

data class JungleDominanceEntry(
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
)

data class JungleDominanceResult(
    val rankings: List<JungleDominanceEntry>,
)

data class SupportImpactEntry(
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
)

data class SupportImpactResult(
    val rankings: List<SupportImpactEntry>,
)

/**
 * 시야 지표.
 *
 * avgSightWardsBought 는 걷어냈다. 구형 와드(sight ward)는 게임에서 사라진 지 오래라
 * sight_wards_bought_in_game 이 전 행 0 이다. 제어 와드는 1,162행에 살아 있어 그대로 둔다.
 */
data class VisionPlayerEntry(
    val riotId: String,
    val games: Int,
    val avgVisionScore: Double,
    val avgWardsPlaced: Double,
    val avgWardsKilled: Double,
    val avgControlWardsBought: Double,
    val wardKillRate: Double,
)

data class VisionDominanceResult(
    val players: List<VisionPlayerEntry>,
)

data class GoldEfficiencyEntry(
    val riotId: String,
    val games: Int,
    val avgDmgPerGold: Double,
    val avgVisionPerGold: Double,
    val avgObjPerGold: Double,
    val avgCsPerGold: Double,
    val goldEfficiencyScore: Double,
    val tags: List<String>,
)

data class GoldEfficiencyResult(
    val rankings: List<GoldEfficiencyEntry>,
    val dmgEfficiencyKing: String?,
    val visionEfficiencyKing: String?,
    val csEfficiencyKing: String?,
    val objEfficiencyKing: String?,
)
