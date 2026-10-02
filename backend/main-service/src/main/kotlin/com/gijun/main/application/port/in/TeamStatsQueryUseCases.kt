package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetDuoStatsQuery
import com.gijun.main.application.dto.query.GetRivalMatchupQuery
import com.gijun.main.application.dto.result.DuoStatsResult
import com.gijun.main.application.dto.result.RivalMatchupResult

interface GetDuoStatsUseCase {
    fun getDuoStats(query: GetDuoStatsQuery): DuoStatsResult
}

interface GetRivalMatchupUseCase {
    fun getRivalMatchup(query: GetRivalMatchupQuery): RivalMatchupResult
}
