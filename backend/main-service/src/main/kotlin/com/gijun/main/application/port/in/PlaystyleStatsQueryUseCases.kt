package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.DefeatContributionResult
import com.gijun.main.application.dto.result.PlaystyleDnaResult
import com.gijun.main.application.dto.result.PositionBadgeResult
import com.gijun.main.application.dto.result.PositionChampionPoolResult
import com.gijun.main.application.dto.result.SurvivalIndexResult
import com.gijun.main.domain.match.enums.GameMode

interface GetPlaystyleDnaUseCase {
    fun getPlaystyleDna(mode: GameMode): PlaystyleDnaResult
}

interface GetPositionBadgeUseCase {
    fun getPositionBadge(mode: GameMode): PositionBadgeResult
}

interface GetPositionChampionPoolUseCase {
    fun getPositionChampionPool(mode: GameMode): PositionChampionPoolResult
}

interface GetSurvivalIndexUseCase {
    fun getSurvivalIndex(mode: GameMode): SurvivalIndexResult
}

interface GetDefeatContributionUseCase {
    fun getDefeatContribution(mode: GameMode): DefeatContributionResult
}
