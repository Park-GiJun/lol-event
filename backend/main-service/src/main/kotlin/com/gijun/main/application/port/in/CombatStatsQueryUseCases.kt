package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.ChaosMatchResult
import com.gijun.main.application.dto.result.DamageAnalysisResult
import com.gijun.main.application.dto.result.LateGameResult
import com.gijun.main.application.dto.result.MultiKillHighlightsResult
import com.gijun.main.application.dto.result.SurrenderAnalysisResult
import com.gijun.main.domain.match.enums.GameMode

interface GetMultiKillHighlightsUseCase {
    fun getMultiKillHighlights(mode: GameMode): MultiKillHighlightsResult
}

interface GetChaosMatchUseCase {
    fun getChaosMatch(mode: GameMode): ChaosMatchResult
}

interface GetDamageAnalysisUseCase {
    fun getDamageAnalysis(mode: GameMode): DamageAnalysisResult
}

interface GetSurrenderAnalysisUseCase {
    fun getSurrenderAnalysis(mode: GameMode): SurrenderAnalysisResult
}

interface GetLateGameUseCase {
    fun getLateGame(mode: GameMode): LateGameResult
}
