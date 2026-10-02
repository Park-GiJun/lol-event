package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetEloHistoryQuery
import com.gijun.main.application.dto.result.EloLeaderboardResult
import com.gijun.main.application.dto.result.PlayerEloHistoryResult
import com.gijun.main.application.dto.result.PlayerRatingResult
import com.gijun.main.shared.domain.vo.RiotId

interface GetRatingsUseCase {
    /** 전원의 두 레이팅 원값. 라인 레이팅 내림차순. */
    fun getRatings(): List<PlayerRatingResult>
}

interface GetRatingUseCase {
    /** 기록이 없는 사람은 시작 점수로 돌려준다 — null 이 아니다. */
    fun getRating(riotId: RiotId): PlayerRatingResult
}

interface GetEloLeaderboardUseCase {
    /** @param minDuels 라인 맞대결이 이 수 미만이면 배치 중으로 분류해 순위에서 뺀다. */
    fun getEloLeaderboard(minDuels: Int): EloLeaderboardResult
}

interface GetEloHistoryUseCase {
    fun getEloHistory(query: GetEloHistoryQuery): PlayerEloHistoryResult
}
