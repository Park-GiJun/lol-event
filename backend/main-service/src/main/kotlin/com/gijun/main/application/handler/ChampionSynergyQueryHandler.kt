package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.AllyChampionStat
import com.gijun.main.application.dto.result.ChampionSynergyResult
import com.gijun.main.application.port.`in`.GetChampionSynergyUseCase
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 같은 팀이었던 챔피언별 전적. "아군이 X 일 때 뭘 고르면 좋은가" 에 답하는 재료다.
 *
 * 표본이 작다는 것을 숨기지 않으려고 판수를 그대로 돌려준다. 내전 200 판에서 특정 조합은 많아야
 * 몇 판이라, 승률만 보면 3 판 3 승이 "100%" 로 보인다.
 */
@Service
@Transactional(readOnly = true)
class ChampionSynergyQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
) : GetChampionSynergyUseCase {
    override fun getChampionSynergy(champion: String): ChampionSynergyResult {
        val games = HashMap<String, Int>()
        val wins = HashMap<String, Int>()
        var appearances = 0

        matchQueryPersistencePort.findAllWithParticipants(GameMode.ALL.queueIds).forEach { match ->
            match.participants.filter { it.champion == champion }.forEach { self ->
                appearances++
                match.participants
                    .filter { it.teamId == self.teamId && it.champion != champion }
                    .forEach { ally ->
                        games.merge(ally.champion, 1, Int::plus)
                        if (self.win) wins.merge(ally.champion, 1, Int::plus)
                    }
            }
        }

        val allies =
            games
                .filterValues { it >= MIN_GAMES }
                .map { (ally, played) ->
                    val won = wins[ally] ?: 0
                    AllyChampionStat(ally, played, won, won * PERCENT / played)
                }.sortedWith(compareByDescending<AllyChampionStat> { it.games }.thenByDescending { it.winRate })
                .take(MAX_ALLIES)
        return ChampionSynergyResult(champion, appearances, allies)
    }

    private companion object {
        /** 한 판짜리 조합은 우연이다. */
        const val MIN_GAMES = 2
        const val MAX_ALLIES = 20
        const val PERCENT = 100
    }
}
