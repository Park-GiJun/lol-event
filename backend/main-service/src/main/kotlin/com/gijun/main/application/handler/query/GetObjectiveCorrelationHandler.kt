package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.stats.result.ObjectiveStat
import com.gijun.main.application.port.`in`.GetObjectiveCorrelationUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetObjectiveCorrelationHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
) : GetObjectiveCorrelationUseCase {

    override fun getObjectiveCorrelation(mode: String): ObjectiveCorrelationResult = cache.getOrCompute("objective-correlation:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))
        val totalGames = matches.size

        if (totalGames == 0) return@getOrCompute ObjectiveCorrelationResult(0, emptyList())

        data class ObjConfig(
            val key: String,
            val label: String,
            val hasFirst: (com.gijun.main.domain.model.match.MatchTeam) -> Boolean,
        )

        val objectives = listOf(
            ObjConfig("firstBlood",     "퍼스트 블러드") { it.firstBlood },
            ObjConfig("firstDragon",    "첫 드래곤")     { it.firstDragon },
            ObjConfig("firstBaron",     "첫 바론")       { it.firstBaron },
            ObjConfig("firstTower",     "첫 포탑")       { it.firstTower },
            ObjConfig("firstInhibitor", "첫 억제기")     { it.firstInhibitor },
        )

        val stats = objectives.mapNotNull { cfg ->
            // firstBlood/firstDragon 등을 가진 팀이 이긴 경기 수 집계
            var gamesWithFirst = 0
            var winsWithFirst  = 0

            for (m in matches) {
                val teamWithFirst = m.teams.firstOrNull { cfg.hasFirst(it) } ?: continue
                gamesWithFirst++
                if (teamWithFirst.win) winsWithFirst++
            }

            if (gamesWithFirst == 0) return@mapNotNull null

            val gamesWithout = totalGames - gamesWithFirst
            val winsWithout  = matches.count { m ->
                val teamWithFirst = m.teams.firstOrNull { cfg.hasFirst(it) }
                teamWithFirst == null && m.teams.any { it.win }
            }

            val wrWithFirst  = winsWithFirst  * 100 / gamesWithFirst
            val wrWithout    = if (gamesWithout > 0) winsWithout * 100 / gamesWithout else 0

            ObjectiveStat(
                objective      = cfg.key,
                label          = cfg.label,
                totalGames     = totalGames,
                gamesWithFirst = gamesWithFirst,
                winsWithFirst  = winsWithFirst,
                winRateWithFirst = wrWithFirst,
                winRateWithout   = wrWithout,
            )
        }

        ObjectiveCorrelationResult(totalGames, stats)
    }
}
