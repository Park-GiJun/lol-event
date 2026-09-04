package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.champion.result.ChampionPageResult
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionPageUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val TIER_MIN_GAMES = 5

@Service
@Transactional(readOnly = true)
class GetChampionPageHandler(
    private val getChampionStatsUseCase: GetChampionStatsUseCase,
    private val getChampionTierUseCase: GetChampionTierUseCase,
    private val getChampionMatchupUseCase: GetChampionMatchupUseCase,
) : GetChampionPageUseCase {

    override fun getChampionPage(champion: String, mode: String): ChampionPageResult {
        val detail = getChampionStatsUseCase.getChampionStats(champion, mode)

        val tier = getChampionTierUseCase.getChampionTier(mode, TIER_MIN_GAMES)
            .tierList.firstOrNull { it.champion == champion }

        val matchup = getChampionMatchupUseCase
            .getMatchup(champion = champion, vsChampion = null, mode = mode)

        // 챔피언 시너지는 걷어냈다. 154경기에서 챔피언 2인 조합은 2,142가지가 나오는데
        // 중앙 표본이 1회, 10회 이상은 단 하나뿐이라 어떤 컷을 걸어도 의미가 생기지 않는다.
        return ChampionPageResult(
            detail = detail,
            tier = tier,
            laneStrength = matchup.laneStrength,
            matchups = matchup.matchups,
            matchupMinGames = matchup.minGames,
        )
    }
}
