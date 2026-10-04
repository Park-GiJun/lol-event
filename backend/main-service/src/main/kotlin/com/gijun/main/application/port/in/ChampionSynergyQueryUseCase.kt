package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetAllyPicksQuery
import com.gijun.main.application.dto.result.AllyPickStat
import com.gijun.main.application.dto.result.ChampionSynergyResult

interface GetChampionSynergyUseCase {
    /**
     * 이 챔피언과 **같은 팀**이었던 챔피언별 전적.
     *
     * @param champion 영문 키(`Seraphine`).
     */
    fun getChampionSynergy(champion: String): ChampionSynergyResult
}

interface GetAllyPicksUseCase {
    /**
     * 한 라인에 섰던 챔피언마다, 물어본 아군들과 **같은 팀**이었던 전적. 보정 승률이 높은 순이다.
     *
     * 아군 하나만 보면 표본이 한두 판이라, 아군 여럿과의 기록을 더해 본다.
     */
    fun getAllyPicks(query: GetAllyPicksQuery): List<AllyPickStat>
}
