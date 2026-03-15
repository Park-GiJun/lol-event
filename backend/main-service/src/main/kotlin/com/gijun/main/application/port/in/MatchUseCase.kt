package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.match.command.SaveMatchesCommand
import com.gijun.main.application.dto.match.result.MatchResult
import com.gijun.main.application.dto.match.result.SaveMatchesResult
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.StatsResult

interface SaveMatchesUseCase {
    fun save(command: SaveMatchesCommand): SaveMatchesResult
}

interface GetMatchesUseCase {
    fun getAll(mode: String): List<MatchResult>
}

interface DeleteMatchUseCase {
    fun delete(matchId: String)
}

interface GetStatsUseCase {
    fun getStats(mode: String): StatsResult
}

interface GetPlayerStatsUseCase {
    fun getPlayerStats(riotId: String, mode: String): PlayerDetailStatsResult
}
