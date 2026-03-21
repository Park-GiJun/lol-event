package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.stats.result.EloLeaderboardResult
import com.gijun.main.application.dto.stats.result.PlayerEloHistoryResult
import com.gijun.main.domain.model.elo.PlayerElo

interface CalculateEloForMatchUseCase {
    fun calculateForMatch(matchId: String)
}

interface ResetAndRecalculateEloUseCase {
    fun resetAndRecalculate()
}

interface GetEloUseCase {
    fun getAll(): List<PlayerElo>
    fun getByRiotId(riotId: String): PlayerElo?
}

interface GetEloLeaderboardUseCase {
    fun getLeaderboard(): EloLeaderboardResult
}

interface GetEloHistoryUseCase {
    fun getHistory(riotId: String, limit: Int = 30): PlayerEloHistoryResult
}
