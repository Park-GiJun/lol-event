package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.*

interface SaveMatchesUseCase {
    fun save(request: SaveMatchesRequest): SaveMatchesResponse
}

interface GetMatchesUseCase {
    fun getAll(mode: String): List<MatchDto>
}

interface DeleteMatchUseCase {
    fun delete(matchId: String)
}

interface GetStatsUseCase {
    fun getStats(mode: String): StatsResponse
}
