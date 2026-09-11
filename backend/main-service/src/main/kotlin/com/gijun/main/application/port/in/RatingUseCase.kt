package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.stats.result.EloLeaderboardResult
import com.gijun.main.application.dto.stats.result.PlayerEloHistoryResult
import com.gijun.main.application.dto.stats.result.RatingValidationResult
import com.gijun.main.application.dto.stats.result.RecalculateResult
import com.gijun.main.domain.model.rating.PlayerRating

interface CalculateRatingForMatchUseCase {
    fun calculateForMatch(matchId: String)
}

interface ResetAndRecalculateRatingUseCase {
    fun resetAndRecalculate(): RecalculateResult
}

interface GetRatingUseCase {
    fun getAll(): List<PlayerRating>
    fun getByRiotId(riotId: String): PlayerRating?
}

interface GetEloLeaderboardUseCase {
    /** @param minDuels 라인 맞대결이 이 수 미만이면 배치 중으로 분류해 순위에서 뺀다. */
    fun getLeaderboard(minDuels: Int): EloLeaderboardResult
}

interface GetEloHistoryUseCase {
    fun getHistory(riotId: String, limit: Int = 30): PlayerEloHistoryResult
}

interface ValidateRatingUseCase {
    /**
     * 워크포워드 평가. 앞 [warmup] 경기로 레이팅을 데운 뒤, 매 경기를 **그 시점 레이팅으로만**
     * 예측하고 나서 반영한다.
     *
     * @param excludeRepeatedTeams 직전 경기와 팀 구성이 같은(진영만 바뀐 경우 포함) 경기를 제외할지.
     *        켜 두는 것이 기본이다 — 자세한 이유는 [com.gijun.main.application.handler.query.RatingValidationHandler] 참고.
     */
    fun validate(warmup: Int, excludeRepeatedTeams: Boolean): RatingValidationResult
}
