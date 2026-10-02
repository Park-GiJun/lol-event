package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.match.result.MatchPageResult
import com.gijun.main.application.dto.match.result.MatchResult
import com.gijun.main.application.dto.match.result.MatchTimelineResult

interface GetMatchesUseCase {
    fun getAll(mode: String): List<MatchResult>

    fun getById(matchId: String): MatchResult?

    /** 경기 목록 화면용. 최신순 한 페이지를 요약 필드만 담아 반환한다. */
    fun getPage(
        mode: String,
        page: Int,
        size: Int,
    ): MatchPageResult
}

interface GetMatchTimelineUseCase {
    /**
     * 경기 한 판의 타임라인. 경기가 없으면 null 이고, **타임라인만 없는 경우는 null 이 아니다** —
     * `hasTimeline = false` 인 빈 결과를 돌려준다. 이유는 [MatchTimelineResult.hasTimeline] 참고.
     */
    fun getMatchTimeline(matchId: String): MatchTimelineResult?
}
