package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.GoldEfficiencyResult
import com.gijun.main.application.dto.result.JungleDominanceResult
import com.gijun.main.application.dto.result.SupportImpactResult
import com.gijun.main.application.dto.result.VisionDominanceResult
import com.gijun.main.domain.match.enums.GameMode

interface GetJungleDominanceUseCase {
    fun getJungleDominance(mode: GameMode): JungleDominanceResult
}

interface GetSupportImpactUseCase {
    fun getSupportImpact(mode: GameMode): SupportImpactResult
}

interface GetVisionDominanceUseCase {
    fun getVisionDominance(mode: GameMode): VisionDominanceResult
}

interface GetGoldEfficiencyUseCase {
    fun getGoldEfficiency(mode: GameMode): GoldEfficiencyResult
}
