package com.gijun.main.application.dto.stats.result

data class EloRankEntry(
    /** 배치 중인 플레이어는 0. 순위를 매기지 않는다. */
    val rank: Int,
    val riotId: String,
    val elo: Double,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val winStreak: Int,
    val lossStreak: Int,
    /** 최소 경기 수 미달. 목록에는 남기되 순위에서는 뺀다. */
    val placement: Boolean,
    /** HIGH / MEDIUM / LOW / INSUFFICIENT */
    val sampleGrade: String,
)

data class EloLeaderboardResult(
    /** 순위가 매겨진 플레이어가 먼저, 배치 중인 플레이어가 뒤에 온다. */
    val players: List<EloRankEntry>,
    /** 순위에 들어가기 위해 필요한 최소 경기 수 */
    val minGames: Int,
    val rankedCount: Int,
    val placementCount: Int,
)
