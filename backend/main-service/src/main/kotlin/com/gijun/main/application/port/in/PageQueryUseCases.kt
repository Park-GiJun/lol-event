package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetChampionPageQuery
import com.gijun.main.application.dto.query.GetSummonerProfileQuery
import com.gijun.main.application.dto.result.ChampionPageResult
import com.gijun.main.application.dto.result.HomeResult
import com.gijun.main.application.dto.result.SummonerProfileResult
import com.gijun.main.domain.match.enums.GameMode

interface GetHomeUseCase {
    /** 홈 화면 한 장에 필요한 것을 한 번에 반환한다. */
    fun getHome(mode: GameMode): HomeResult
}

interface GetChampionPageUseCase {
    /** 챔피언 화면 한 장에 필요한 것을 한 번에 반환한다. */
    fun getChampionPage(query: GetChampionPageQuery): ChampionPageResult
}

interface GetSummonerProfileUseCase {
    /** 소환사 화면 한 장에 필요한 것을 한 번에 반환한다. */
    fun getSummonerProfile(query: GetSummonerProfileQuery): SummonerProfileResult
}
