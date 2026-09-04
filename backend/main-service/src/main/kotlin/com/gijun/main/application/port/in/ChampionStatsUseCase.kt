package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.stats.result.BanAnalysisResult
import com.gijun.main.application.dto.stats.result.ChampionCertificateResult
import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionMatchupResult
import com.gijun.main.application.dto.stats.result.ChampionTierResult

interface GetChampionStatsUseCase {
    fun getChampionStats(champion: String, mode: String): ChampionDetailStats
}

interface GetChampionMatchupUseCase {
    fun getMatchup(champion: String?, vsChampion: String?, mode: String): ChampionMatchupResult
}

interface GetChampionCertificateUseCase {
    fun getChampionCertificates(mode: String, minGames: Int = 3): ChampionCertificateResult
}

interface GetChampionTierUseCase {
    fun getChampionTier(mode: String, minGames: Int = 3): ChampionTierResult
}

interface GetBanAnalysisUseCase {
    fun getBanAnalysis(mode: String): BanAnalysisResult
}
