package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.champion.result.ChampionPageResult
import com.gijun.main.application.dto.home.result.HomeResult

interface GetHomeUseCase {
    /** 홈 화면 한 장에 필요한 것을 한 번에 반환한다. */
    fun getHome(mode: String): HomeResult
}

interface GetChampionPageUseCase {
    /** 챔피언 화면 한 장에 필요한 것을 한 번에 반환한다. */
    fun getChampionPage(champion: String, mode: String): ChampionPageResult
}
