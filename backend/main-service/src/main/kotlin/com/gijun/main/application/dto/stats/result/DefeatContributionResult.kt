package com.gijun.main.application.dto.stats.result

data class DefeatContributionEntry(
    val riotId: String,
    val games: Int,           // 전체 게임
    val losses: Int,          // 패배 게임
    val avgDefeatScore: Double,
    val avgDeaths: Double,
    val avgDamage: Double,
    val worstMatch: String?,  // 가장 높은 defeat_score 경기의 matchId
)

data class DefeatContributionResult(
    val rankings: List<DefeatContributionEntry>,
)
