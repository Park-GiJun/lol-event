package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetMatchPageQuery
import com.gijun.main.application.dto.result.MatchPageResult
import com.gijun.main.application.dto.result.MatchResult
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.domain.vo.MatchId

interface GetMatchesUseCase {
    fun getMatches(mode: GameMode): List<MatchResult>
}

interface GetMatchUseCase {
    /** 경기가 없으면 null. */
    fun getMatch(matchId: MatchId): MatchResult?
}

interface GetMatchPageUseCase {
    /** 경기 목록 화면용. 최신순 한 페이지를 요약 필드만 담아 반환한다. */
    fun getMatchPage(query: GetMatchPageQuery): MatchPageResult
}
