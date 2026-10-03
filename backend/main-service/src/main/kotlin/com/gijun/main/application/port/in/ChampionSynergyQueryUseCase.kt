package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.ChampionSynergyResult

interface GetChampionSynergyUseCase {
    /**
     * 이 챔피언과 **같은 팀**이었던 챔피언별 전적.
     *
     * @param champion 영문 키(`Seraphine`).
     */
    fun getChampionSynergy(champion: String): ChampionSynergyResult
}
