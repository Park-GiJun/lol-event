package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.champion.result.ChampionPageResult
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionPageUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetChampionSynergyUseCase
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val TIER_MIN_GAMES = 5
private const val SYNERGY_MIN_GAMES = 2
private const val TOP_SYNERGIES = 20

@Service
@Transactional(readOnly = true)
class GetChampionPageHandler(
    private val getChampionStatsUseCase: GetChampionStatsUseCase,
    private val getChampionTierUseCase: GetChampionTierUseCase,
    private val getChampionMatchupUseCase: GetChampionMatchupUseCase,
    private val getChampionSynergyUseCase: GetChampionSynergyUseCase,
) : GetChampionPageUseCase {

    override fun getChampionPage(champion: String, mode: String): ChampionPageResult {
        val detail = getChampionStatsUseCase.getChampionStats(champion, mode)

        val tier = getChampionTierUseCase.getChampionTier(mode, TIER_MIN_GAMES)
            .tierList.firstOrNull { it.champion == champion }

        val matchups = getChampionMatchupUseCase
            .getMatchup(champion = champion, vsChampion = null, mode = mode)
            .matchups

        // 시너지는 champion1/champion2 어느 쪽에 있을지 모른다. 상대 챔피언 관점으로 뒤집어 담는다.
        val synergies = getChampionSynergyUseCase.getChampionSynergy(mode, SYNERGY_MIN_GAMES)
            .synergies
            .filter { it.champion1 == champion || it.champion2 == champion }
            .sortedByDescending { it.games }
            .take(TOP_SYNERGIES)

        return ChampionPageResult(
            detail = detail,
            tier = tier,
            matchups = matchups,
            synergies = synergies,
        )
    }
}
