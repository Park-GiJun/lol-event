package com.gijun.main.application.dto.stats.result

/**
 * 후반 활약 지표.
 *
 * firstInhibitorRate 는 걷어냈다. first_inhibitor_kill / first_inhibitor_assist 가
 * 수집분 1,716행 전부 false 라 전원 0.0 으로 나란히 서던 칸이다.
 */
data class LateGamePlayerEntry(
    val riotId: String,
    val games: Int,
    val avgInhibitorKills: Double,
    val avgChampLevel: Double,
    val avgLongestTimeSpentLiving: Int,
    val avgLargestKillingSpree: Double,
    val avgLargestMultiKill: Double,
    val lateGameScore: Double,
)

data class LateGameResult(
    val players: List<LateGamePlayerEntry>,
)
