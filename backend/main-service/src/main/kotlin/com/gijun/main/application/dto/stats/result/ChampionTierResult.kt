package com.gijun.main.application.dto.stats.result

data class ChampionTierEntry(
    val champion: String,
    val championId: Int,
    val tier: String,
    val tierScore: Double,
    val games: Int,
    /** 관측 승률. 표본이 작으면 그대로 믿으면 안 된다. 항상 games 와 같이 보여줄 것. */
    val winRate: Int,
    /** 전체 평균 쪽으로 끌어당긴 승률. 정렬은 이 값으로 한다. */
    val adjustedWinRate: Double,
    /** HIGH / MEDIUM / LOW / INSUFFICIENT */
    val sampleGrade: String,
    val kda: Double,
    val pickRate: Double,
    val avgDamage: Double,
)

data class ChampionTierResult(
    val tierList: List<ChampionTierEntry>,
    val byTier: Map<String, List<ChampionTierEntry>>,
    val totalMatches: Int,
)
