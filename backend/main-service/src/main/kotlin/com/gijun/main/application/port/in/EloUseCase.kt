package com.gijun.main.application.port.`in`

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
