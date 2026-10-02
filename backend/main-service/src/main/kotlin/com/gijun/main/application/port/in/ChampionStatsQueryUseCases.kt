package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetChampionCertificateQuery
import com.gijun.main.application.dto.query.GetChampionMatchupQuery
import com.gijun.main.application.dto.query.GetChampionStatsQuery
import com.gijun.main.application.dto.query.GetChampionTierQuery
import com.gijun.main.application.dto.result.BanAnalysisResult
import com.gijun.main.application.dto.result.ChampionCertificateResult
import com.gijun.main.application.dto.result.ChampionDetailStats
import com.gijun.main.application.dto.result.ChampionMatchupResult
import com.gijun.main.application.dto.result.ChampionTierResult
import com.gijun.main.domain.match.enums.GameMode

interface GetChampionStatsUseCase {
    fun getChampionStats(query: GetChampionStatsQuery): ChampionDetailStats
}

interface GetChampionMatchupUseCase {
    fun getChampionMatchup(query: GetChampionMatchupQuery): ChampionMatchupResult
}

interface GetChampionCertificateUseCase {
    fun getChampionCertificate(query: GetChampionCertificateQuery): ChampionCertificateResult
}

interface GetChampionTierUseCase {
    fun getChampionTier(query: GetChampionTierQuery): ChampionTierResult
}

interface GetBanAnalysisUseCase {
    fun getBanAnalysis(mode: GameMode): BanAnalysisResult
}
