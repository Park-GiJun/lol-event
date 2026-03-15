package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.match.command.SaveMatchesCommand
import com.gijun.main.application.dto.match.result.MatchResult
import com.gijun.main.application.dto.match.result.SaveMatchesResult
import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionSynergyResult
import com.gijun.main.application.dto.stats.result.DuoStatsResult
import com.gijun.main.application.dto.stats.result.MvpStatsResult
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.StatsResult
import com.gijun.main.application.dto.stats.result.StreakResult

interface SaveMatchesUseCase {
    fun save(command: SaveMatchesCommand): SaveMatchesResult
}

interface GetMatchesUseCase {
    fun getAll(mode: String): List<MatchResult>
    fun getById(matchId: String): MatchResult?
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

interface GetOverviewStatsUseCase {
    fun getOverviewStats(mode: String): OverviewStats
}

interface GetChampionStatsUseCase {
    fun getChampionStats(champion: String, mode: String): ChampionDetailStats
}

interface GetChampionSynergyUseCase {
    fun getChampionSynergy(mode: String, minGames: Int): ChampionSynergyResult
}

interface GetDuoStatsUseCase {
    fun getDuoStats(mode: String, minGames: Int): DuoStatsResult
}

interface GetPlayerStreakUseCase {
    fun getPlayerStreak(riotId: String, mode: String): StreakResult
}

interface GetMvpStatsUseCase {
    fun getMvpStats(mode: String): MvpStatsResult
}
