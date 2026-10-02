package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetLaneLeaderboardQuery
import com.gijun.main.application.dto.result.KillParticipationResult
import com.gijun.main.application.dto.result.LaneLeaderboardResult
import com.gijun.main.application.dto.result.MvpStatsResult
import com.gijun.main.domain.match.enums.GameMode

interface GetLaneLeaderboardUseCase {
    fun getLaneLeaderboard(query: GetLaneLeaderboardQuery): LaneLeaderboardResult
}

interface GetMvpStatsUseCase {
    fun getMvpStats(mode: GameMode): MvpStatsResult
}

interface GetKillParticipationUseCase {
    fun getKillParticipation(mode: GameMode): KillParticipationResult
}
