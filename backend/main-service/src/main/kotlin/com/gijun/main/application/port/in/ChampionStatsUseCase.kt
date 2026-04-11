package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.stats.result.BanAnalysisResult
import com.gijun.main.application.dto.stats.result.ChampionCertificateResult
import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionMatchupResult
import com.gijun.main.application.dto.stats.result.ChampionSynergyResult
import com.gijun.main.application.dto.stats.result.ChampionTierResult
import com.gijun.main.application.dto.stats.result.MetaShiftResult

interface GetChampionStatsUseCase {
    fun getChampionStats(champion: String, mode: String): ChampionDetailStats
}

interface GetChampionSynergyUseCase {
    fun getChampionSynergy(mode: String, minGames: Int): ChampionSynergyResult
}

interface GetChampionMatchupUseCase {
    fun getMatchup(champion: String?, vsChampion: String?, mode: String, samePosition: Boolean = false): ChampionMatchupResult
}

interface GetChampionCertificateUseCase {
    fun getChampionCertificates(mode: String, minGames: Int = 5): ChampionCertificateResult
}

interface GetChampionTierUseCase {
    fun getChampionTier(mode: String, minGames: Int = 3): ChampionTierResult
}

interface GetMetaShiftUseCase {
    fun getMetaShift(mode: String): MetaShiftResult
}

interface GetBanAnalysisUseCase {
    fun getBanAnalysis(mode: String): BanAnalysisResult
}
