package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.match.result.MatchPageResult
import com.gijun.main.application.dto.match.result.MatchResult

interface GetMatchesUseCase {
    fun getAll(mode: String): List<MatchResult>
    fun getById(matchId: String): MatchResult?

    /** 경기 목록 화면용. 최신순 한 페이지를 요약 필드만 담아 반환한다. */
    fun getPage(mode: String, page: Int, size: Int): MatchPageResult
}
