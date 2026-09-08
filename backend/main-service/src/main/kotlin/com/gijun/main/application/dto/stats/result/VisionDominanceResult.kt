package com.gijun.main.application.dto.stats.result

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
