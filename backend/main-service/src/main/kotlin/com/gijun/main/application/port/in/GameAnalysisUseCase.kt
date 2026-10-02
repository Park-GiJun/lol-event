package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.ChaosMatchResult
import com.gijun.main.application.dto.result.ComebackIndexResult
import com.gijun.main.application.dto.result.DamageAnalysisResult
import com.gijun.main.application.dto.result.DefeatContributionResult
import com.gijun.main.application.dto.result.EarlyGameDominanceResult
import com.gijun.main.application.dto.result.GameLengthTendencyResult
import com.gijun.main.application.dto.result.GoldEfficiencyResult
import com.gijun.main.application.dto.result.JungleDominanceResult
import com.gijun.main.application.dto.result.KillParticipationResult
import com.gijun.main.application.dto.result.LaneLeaderboardResult
import com.gijun.main.application.dto.result.LateGameResult
import com.gijun.main.application.dto.result.MultiKillHighlightsResult
import com.gijun.main.application.dto.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.result.PositionBadgeResult
import com.gijun.main.application.dto.result.PositionChampionPoolResult
import com.gijun.main.application.dto.result.RivalMatchupResult
import com.gijun.main.application.dto.result.SessionDetailResult
import com.gijun.main.application.dto.result.SessionReportResult
import com.gijun.main.application.dto.result.SupportImpactResult
import com.gijun.main.application.dto.result.SurrenderAnalysisResult
import com.gijun.main.application.dto.result.SurvivalIndexResult
import com.gijun.main.application.dto.result.TimePatternResult
import com.gijun.main.application.dto.result.VisionDominanceResult
import com.gijun.main.application.dto.result.WeeklyAwardsResult

interface GetLaneLeaderboardUseCase {
    fun getLaneLeaderboard(
        lane: String,
        mode: String,
    ): LaneLeaderboardResult
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
    fun getRivalMatchups(
        mode: String,
        minGames: Int = 3,
    ): RivalMatchupResult
}

interface GetPositionBadgeUseCase {
    fun getPositionBadge(mode: String): PositionBadgeResult
}

interface GetSessionReportUseCase {
    fun getSessionReport(mode: String): SessionReportResult
}

interface GetSessionDetailUseCase {
    /**
     * 하루치 내전 상세. [date] 는 `yyyy-MM-dd`.
     *
     * 그 세션에 경기가 없으면 null 이다 — 세션 목록에서 넘어오는 화면이라 없는 날짜는 실제
     * 오류이고, 404 가 행동으로 이어진다. (경기 타임라인과 다른 판단이다. 거기는 타임라인이
     * 없는 경기가 정상이라 빈 결과를 준다.)
     */
    fun getSessionDetail(
        date: String,
        mode: String,
    ): SessionDetailResult?
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

interface GetDamageAnalysisUseCase {
    fun getDamageAnalysis(mode: String): DamageAnalysisResult
}

interface GetVisionDominanceUseCase {
    fun getVisionDominance(mode: String): VisionDominanceResult
}

interface GetSurrenderAnalysisUseCase {
    fun getSurrenderAnalysis(mode: String): SurrenderAnalysisResult
}

interface GetLateGameUseCase {
    fun getLateGame(mode: String): LateGameResult
}
