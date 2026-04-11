package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.stats.result.ChaosMatchResult
import com.gijun.main.application.dto.stats.result.ComebackIndexResult
import com.gijun.main.application.dto.stats.result.DefeatContributionResult
import com.gijun.main.application.dto.stats.result.EarlyGameDominanceResult
import com.gijun.main.application.dto.stats.result.GameLengthTendencyResult
import com.gijun.main.application.dto.stats.result.GoldEfficiencyResult
import com.gijun.main.application.dto.stats.result.JungleDominanceResult
import com.gijun.main.application.dto.stats.result.KillParticipationResult
import com.gijun.main.application.dto.stats.result.LaneLeaderboardResult
import com.gijun.main.application.dto.stats.result.MultiKillHighlightsResult
import com.gijun.main.application.dto.stats.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.stats.result.PositionBadgeResult
import com.gijun.main.application.dto.stats.result.PositionChampionPoolResult
import com.gijun.main.application.dto.stats.result.RivalMatchupResult
import com.gijun.main.application.dto.stats.result.SessionReportResult
import com.gijun.main.application.dto.stats.result.SupportImpactResult
import com.gijun.main.application.dto.stats.result.SurvivalIndexResult
import com.gijun.main.application.dto.stats.result.TeamChemistryResult
import com.gijun.main.application.dto.stats.result.TimePatternResult
import com.gijun.main.application.dto.stats.result.WeeklyAwardsResult

interface GetLaneLeaderboardUseCase {
    fun getLaneLeaderboard(lane: String, mode: String): LaneLeaderboardResult
}

interface GetObjectiveCorrelationUseCase {
    fun getObjectiveCorrelation(mode: String): ObjectiveCorrelationResult
}

interface GetWeeklyAwardsUseCase {
    fun getWeeklyAwards(mode: String): WeeklyAwardsResult
}

interface GetDefeatContributionUseCase {
    fun getDefeatContribution(mode: String): DefeatContributionResult
}

interface GetMultiKillHighlightsUseCase {
    fun getMultiKillHighlights(mode: String): MultiKillHighlightsResult
}

interface GetChaosMatchUseCase {
    fun getChaosMatch(mode: String): ChaosMatchResult
}

interface GetSurvivalIndexUseCase {
    fun getSurvivalIndex(mode: String): SurvivalIndexResult
}

interface GetJungleDominanceUseCase {
    fun getJungleDominance(mode: String): JungleDominanceResult
}

interface GetSupportImpactUseCase {
    fun getSupportImpact(mode: String): SupportImpactResult
}

interface GetRivalMatchupUseCase {
    fun getRivalMatchups(mode: String, minGames: Int = 3): RivalMatchupResult
}

interface GetTeamChemistryUseCase {
    fun getTeamChemistry(mode: String, minGames: Int = 3): TeamChemistryResult
}

interface GetPositionBadgeUseCase {
    fun getPositionBadge(mode: String): PositionBadgeResult
}

interface GetSessionReportUseCase {
    fun getSessionReport(mode: String): SessionReportResult
}

interface GetGameLengthTendencyUseCase {
    fun getGameLengthTendency(mode: String): GameLengthTendencyResult
}

interface GetEarlyGameDominanceUseCase {
    fun getEarlyGameDominance(mode: String): EarlyGameDominanceResult
}

interface GetComebackIndexUseCase {
    fun getComebackIndex(mode: String): ComebackIndexResult
}

interface GetGoldEfficiencyUseCase {
    fun getGoldEfficiency(mode: String): GoldEfficiencyResult
}

interface GetTimePatternUseCase {
    fun getTimePattern(mode: String): TimePatternResult
}

interface GetKillParticipationUseCase {
    fun getKillParticipation(mode: String): KillParticipationResult
}

interface GetPositionChampionPoolUseCase {
    fun getPositionChampionPool(mode: String): PositionChampionPoolResult
}
