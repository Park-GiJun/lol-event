package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.match.command.SaveMatchesCommand
import com.gijun.main.application.dto.match.result.MatchResult
import com.gijun.main.application.dto.match.result.SaveMatchesResult
import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionMatchupResult
import com.gijun.main.application.dto.stats.result.ChampionSynergyResult
import com.gijun.main.application.dto.stats.result.DuoStatsResult
import com.gijun.main.application.dto.stats.result.LaneLeaderboardResult
import com.gijun.main.application.dto.stats.result.MvpStatsResult
import com.gijun.main.application.dto.stats.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.StatsResult
import com.gijun.main.application.dto.stats.result.StreakResult
import com.gijun.main.application.dto.stats.result.WeeklyAwardsResult
import com.gijun.main.application.dto.stats.result.DefeatContributionResult
import com.gijun.main.application.dto.stats.result.MultiKillHighlightsResult
import com.gijun.main.application.dto.stats.result.ChaosMatchResult
import com.gijun.main.application.dto.stats.result.GrowthCurveResult
import com.gijun.main.application.dto.stats.result.SurvivalIndexResult
import com.gijun.main.application.dto.stats.result.JungleDominanceResult
import com.gijun.main.application.dto.stats.result.SupportImpactResult
import com.gijun.main.application.dto.stats.result.RivalMatchupResult
import com.gijun.main.application.dto.stats.result.TeamChemistryResult
import com.gijun.main.application.dto.stats.result.PositionBadgeResult
import com.gijun.main.application.dto.stats.result.ChampionCertificateResult
import com.gijun.main.application.dto.stats.result.PlaystyleDnaResult
import com.gijun.main.application.dto.stats.result.MetaShiftResult

interface SaveMatchesUseCase {
    fun save(command: SaveMatchesCommand): SaveMatchesResult
}

interface GetMatchesUseCase {
    fun getAll(mode: String): List<MatchResult>
    fun getById(matchId: String): MatchResult?
}

interface DeleteMatchUseCase {
    fun delete(matchId: String)
}

interface GetStatsUseCase {
    fun getStats(mode: String): StatsResult
}

interface GetPlayerStatsUseCase {
    fun getPlayerStats(riotId: String, mode: String, lane: String? = null): PlayerDetailStatsResult
}

interface GetOverviewStatsUseCase {
    fun getOverviewStats(mode: String): OverviewStats
}

interface GetChampionStatsUseCase {
    fun getChampionStats(champion: String, mode: String): ChampionDetailStats
}

interface GetChampionSynergyUseCase {
    fun getChampionSynergy(mode: String, minGames: Int): ChampionSynergyResult
}

interface GetDuoStatsUseCase {
    fun getDuoStats(mode: String, minGames: Int): DuoStatsResult
}

interface GetPlayerStreakUseCase {
    fun getPlayerStreak(riotId: String, mode: String): StreakResult
}

interface GetMvpStatsUseCase {
    fun getMvpStats(mode: String): MvpStatsResult
}

interface GetLaneLeaderboardUseCase {
    fun getLaneLeaderboard(lane: String, mode: String): LaneLeaderboardResult
}

interface GetChampionMatchupUseCase {
    /** champion: X의 전적(X가 플레이될 때 각 상대 챔피언 vs) / vsChampion: X를 상대한 챔피언 전적(카운터픽)
     *  samePosition=true: 같은 라인 상대에 대해서만 계산 */
    fun getMatchup(champion: String?, vsChampion: String?, mode: String, samePosition: Boolean = false): ChampionMatchupResult
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

interface GetGrowthCurveUseCase {
    fun getGrowthCurve(riotId: String, mode: String): GrowthCurveResult
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

interface GetChampionCertificateUseCase {
    fun getChampionCertificates(mode: String, minGames: Int = 5): ChampionCertificateResult
}

interface GetPlaystyleDnaUseCase {
    fun getPlaystyleDna(mode: String): PlaystyleDnaResult
}

interface GetMetaShiftUseCase {
    fun getMetaShift(mode: String): MetaShiftResult
}
