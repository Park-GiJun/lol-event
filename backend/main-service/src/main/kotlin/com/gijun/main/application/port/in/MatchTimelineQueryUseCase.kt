package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.MatchTimelineResult
import com.gijun.main.shared.domain.vo.MatchId

interface GetMatchTimelineUseCase {
    /**
     * 경기 한 판의 타임라인. 경기가 없으면 null 이고, **타임라인만 없는 경우는 null 이 아니다** —
     * `hasTimeline = false` 인 빈 결과를 돌려준다. 이유는 [MatchTimelineResult.hasTimeline] 참고.
     */
    fun getMatchTimeline(matchId: MatchId): MatchTimelineResult?
}
