package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.stats.result.ObjectiveStat
import com.gijun.main.application.port.`in`.GetObjectiveCorrelationUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchTeam
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetObjectiveCorrelationHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetObjectiveCorrelationUseCase {

    /**
     * 오브젝트 하나를 어느 팀이 챙겼는지 가리는 방법.
     *
     * 퍼블·첫 드래곤 같은 건 "먼저" 잡은 팀이 플래그로 기록된다.
     * 공허 유충은 그런 플래그가 없어서(horde_kills 카운트만 있다) 더 많이 먹은 쪽을 주인으로 본다.
     */
    private sealed interface Basis {
        /** 플래그가 켜진 팀. */
        data class First(val flag: (MatchTeam) -> Boolean) : Basis

        /** 더 많이 챙긴 팀. 같으면 주인을 못 가린다. */
        data class Majority(val count: (MatchTeam) -> Int) : Basis
    }

    private data class ObjConfig(val key: String, val label: String, val basis: Basis)

    private val objectives = listOf(
        ObjConfig("firstBlood",     "퍼스트 블러드", Basis.First { it.firstBlood }),
        ObjConfig("firstDragon",    "첫 드래곤",    Basis.First { it.firstDragon }),
        ObjConfig("firstBaron",     "첫 바론",      Basis.First { it.firstBaron }),
        ObjConfig("firstTower",     "첫 포탑",      Basis.First { it.firstTower }),
        ObjConfig("firstInhibitor", "첫 억제기",    Basis.First { it.firstInhibitor }),
        // 공허 유충은 먼저 잡은 팀 표시가 없어 더 많이 먹은 쪽으로 본다.
        // 수집분 346팀 중 181팀에 값이 있다.
        ObjConfig("hordeKills",     "공허 유충",    Basis.Majority { it.hordeKills }),
    )

    /** 이 경기에서 해당 오브젝트를 챙긴 팀. 못 가리면 null. */
    private fun ownerOf(match: Match, basis: Basis): MatchTeam? = when (basis) {
        is Basis.First -> match.teams.firstOrNull { basis.flag(it) }
        is Basis.Majority -> {
            val ranked = match.teams.sortedByDescending { basis.count(it) }
            val top = ranked.firstOrNull()
            val runnerUp = ranked.getOrNull(1)
            // 아무도 안 먹었거나 양 팀이 같으면 주인이 없다.
            when {
                top == null || basis.count(top) == 0 -> null
                runnerUp != null && basis.count(runnerUp) == basis.count(top) -> null
                else -> top
            }
        }
    }

    private fun basisName(basis: Basis) = when (basis) {
        is Basis.First -> "FIRST"
        is Basis.Majority -> "MAJORITY"
    }

    override fun getObjectiveCorrelation(mode: String): ObjectiveCorrelationResult =
        cache.getOrCompute("objective-correlation:$mode") {
            val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))
            val totalGames = matches.size

            if (totalGames == 0) return@getOrCompute ObjectiveCorrelationResult(0, emptyList())

            val stats = objectives.mapNotNull { cfg ->
                var gamesWithOwner = 0
                var winsWithOwner = 0

                for (m in matches) {
                    val owner = ownerOf(m, cfg.basis) ?: continue
                    gamesWithOwner++
                    if (owner.win) winsWithOwner++
                }

                // 이 오브젝트가 한 번도 기록되지 않았으면 화면에 줄을 만들지 않는다.
                // 0% 짜리 빈 칸이 하나 더 생기는 것뿐이라서다.
                if (gamesWithOwner == 0) return@mapNotNull null

                // 주인을 가린 경기에서 "챙긴 팀이 진" 비율이 곧 반대쪽 승률이다.
                // 예전에는 주인이 없는 경기를 세면서 승자 유무만 보고 있어서, 주인이 가려진
                // 경기의 반대편이 통째로 빠졌다.
                val lossesWithOwner = gamesWithOwner - winsWithOwner

                ObjectiveStat(
                    objective        = cfg.key,
                    label            = cfg.label,
                    basis            = basisName(cfg.basis),
                    totalGames       = totalGames,
                    gamesWithFirst   = gamesWithOwner,
                    winsWithFirst    = winsWithOwner,
                    winRateWithFirst = winsWithOwner * 100 / gamesWithOwner,
                    winRateWithout   = lossesWithOwner * 100 / gamesWithOwner,
                )
            }

            ObjectiveCorrelationResult(totalGames, stats)
        }
}
