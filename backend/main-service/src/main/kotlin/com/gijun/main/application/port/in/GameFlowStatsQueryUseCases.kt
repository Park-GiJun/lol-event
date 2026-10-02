package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.ComebackIndexResult
import com.gijun.main.application.dto.result.EarlyGameDominanceResult
import com.gijun.main.application.dto.result.GameLengthTendencyResult
import com.gijun.main.application.dto.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.result.TimePatternResult
import com.gijun.main.domain.match.enums.GameMode

interface GetTimePatternUseCase {
    fun getTimePattern(mode: GameMode): TimePatternResult
}

interface GetGameLengthTendencyUseCase {
    fun getGameLengthTendency(mode: GameMode): GameLengthTendencyResult
}

interface GetEarlyGameDominanceUseCase {
    fun getEarlyGameDominance(mode: GameMode): EarlyGameDominanceResult
}

interface GetComebackIndexUseCase {
    fun getComebackIndex(mode: GameMode): ComebackIndexResult
}

interface GetObjectiveCorrelationUseCase {
    fun getObjectiveCorrelation(mode: GameMode): ObjectiveCorrelationResult
}
