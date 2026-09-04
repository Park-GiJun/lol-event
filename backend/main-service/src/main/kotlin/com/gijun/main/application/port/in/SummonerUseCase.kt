package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.summoner.result.SummonerProfileResult

interface GetSummonerProfileUseCase {
    /** 소환사 화면 한 장에 필요한 것을 한 번에 반환한다. */
    fun getProfile(riotId: String, mode: String): SummonerProfileResult
}
