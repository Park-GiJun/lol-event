package com.gijun.main.shared.infrastructure.web.common

import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.InvalidRequestException
import com.gijun.main.shared.domain.vo.MatchId
import com.gijun.main.shared.domain.vo.Puuid
import com.gijun.main.shared.domain.vo.RiotId
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/*
 * 경로·쿼리로 들어온 문자열을 값 객체로 올린다.
 *
 * 값 객체의 생성자는 `require` 로 불변식을 지키는데, 그 예외(IllegalArgumentException)가 그대로
 * 새면 catch-all 이 500 으로 만든다. 호출자 잘못이므로 여기서 400 으로 바꾼다.
 */

fun riotIdOf(raw: String): RiotId = identifier("riotId", raw, ::RiotId)

fun matchIdOf(raw: String): MatchId = identifier("matchId", raw, ::MatchId)

fun puuidOf(raw: String): Puuid = identifier("puuid", raw, ::Puuid)

/**
 * URL 인코딩이 한 겹 더 걸려 온 값을 푼다.
 *
 * Riot ID 에는 `#` 와 공백이 들어가고 챔피언 이름에도 인코딩이 걸려 온다. Spring 이 경로 변수를
 * 한 번 풀어 주지만, 일부 클라이언트가 한 번 더 인코딩해 보내 온 이력이 있어 예전부터 어댑터가
 * 한 번 더 풀고 있었다. 이미 풀린 값에는 영향이 없다(`%` 가 없으면 그대로다).
 */
fun urlDecoded(raw: String): String = URLDecoder.decode(raw, StandardCharsets.UTF_8)

private fun <T> identifier(
    name: String,
    raw: String,
    create: (String) -> T,
): T =
    runCatching { create(raw) }
        .getOrElse { throw InvalidRequestException("$name 형식이 올바르지 않다.", ErrorCode.INVALID_IDENTIFIER) }
