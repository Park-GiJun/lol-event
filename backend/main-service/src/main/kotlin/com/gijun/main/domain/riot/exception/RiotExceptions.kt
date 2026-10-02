package com.gijun.main.domain.riot.exception

import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.ForbiddenException
import com.gijun.main.shared.domain.exception.NotFoundException

class RiotAccountNotFoundException(
    riotId: String,
) : NotFoundException("라이엇 계정을 찾을 수 없습니다: $riotId")

/** 개발용 Riot API 키는 24 시간마다 만료된다. 키를 갱신하기 전에는 재시도해도 풀리지 않는다. */
class RiotApiKeyExpiredException : ForbiddenException("Riot API 키가 만료됐습니다", ErrorCode.RIOT_API_KEY_EXPIRED)
