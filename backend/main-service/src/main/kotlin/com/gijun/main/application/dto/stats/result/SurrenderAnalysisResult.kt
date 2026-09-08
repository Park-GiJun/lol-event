package com.gijun.main.application.dto.stats.result

/**
 * 항복 분석.
 *
 * 조기 항복(15분 이전 만장일치 항복) 관련 필드를 전부 걷어냈다.
 * game_ended_in_early_surrender / caused_early_surrender / early_surrender_accomplice /
 * team_early_surrendered 가 수집분 1,716행 전부 false 라 화면에 0 만 깔려 있었다.
 * 일반 항복(game_ended_in_surrender)은 158행에 살아 있어 그대로 둔다.
 */
data class SurrenderPlayerEntry(
    val riotId: String,
    val games: Int,
    val surrenderGames: Int,
    val surrenderRate: Double,
)

data class SurrenderAnalysisResult(
    val totalGames: Int,
    val surrenderGames: Int,
    val overallSurrenderRate: Double,
    val players: List<SurrenderPlayerEntry>,
)
