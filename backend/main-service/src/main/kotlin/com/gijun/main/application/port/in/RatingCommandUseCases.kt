package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.RecalculateResult
import com.gijun.main.shared.domain.vo.MatchId

interface CalculateRatingForMatchUseCase {
    fun calculateRatingForMatch(matchId: MatchId)
}

interface ResetAndRecalculateRatingUseCase {
    fun resetAndRecalculateRating(): RecalculateResult
}
