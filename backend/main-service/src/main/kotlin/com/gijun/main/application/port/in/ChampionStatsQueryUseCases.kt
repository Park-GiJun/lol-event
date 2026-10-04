package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetChampionCertificateQuery
import com.gijun.main.application.dto.query.GetChampionMatchupQuery
import com.gijun.main.application.dto.query.GetChampionStatsQuery
import com.gijun.main.application.dto.query.GetChampionTierQuery
import com.gijun.main.application.dto.query.GetLaneChampionsQuery
import com.gijun.main.application.dto.result.BanAnalysisResult
import com.gijun.main.application.dto.result.ChampionCertificateResult
import com.gijun.main.application.dto.result.ChampionDetailStats
import com.gijun.main.application.dto.result.ChampionLaneStrength
import com.gijun.main.application.dto.result.ChampionMatchupResult
import com.gijun.main.application.dto.result.ChampionTierResult
import com.gijun.main.domain.match.enums.GameMode

interface GetChampionStatsUseCase {
    fun getChampionStats(query: GetChampionStatsQuery): ChampionDetailStats
}

interface GetChampionMatchupUseCase {
    fun getChampionMatchup(query: GetChampionMatchupQuery): ChampionMatchupResult
}

interface GetLaneChampionsUseCase {
    /**
     * 그 라인에 선 챔피언 전부. 보정 승률이 높은 순이다.
     *
     * 판수는 라인 맞대결(양 팀에 그 라인이 정확히 한 명씩인 경기)로 센다 — 챔피언 화면의 라인별 지표와 같은 값이다.
     */
    fun getLaneChampions(query: GetLaneChampionsQuery): List<ChampionLaneStrength>
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
