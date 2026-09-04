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
    /** @param minGames 이 경기 수 미만은 배치 중으로 분류해 순위에서 뺀다. */
    fun getLeaderboard(minGames: Int): EloLeaderboardResult
}

interface GetEloHistoryUseCase {
    fun getHistory(riotId: String, limit: Int = 30): PlayerEloHistoryResult
}
