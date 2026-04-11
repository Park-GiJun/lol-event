package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.LateGamePlayerEntry
import com.gijun.main.application.dto.stats.result.LateGameResult
import com.gijun.main.application.port.`in`.GetLateGameUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetLateGameHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetLateGameUseCase {

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getLateGame(mode: String): LateGameResult = cache.getOrCompute("late-game:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class Acc(
            var games: Int = 0,
            var totalInhibitorKills: Int = 0,
            var firstInhibitorGames: Int = 0,
            var totalChampLevel: Int = 0,
            var totalLongestTimeSpentLiving: Long = 0,
            var totalLargestKillingSpree: Int = 0,
            var totalLargestMultiKill: Int = 0,
        )

        val accMap = mutableMapOf<String, Acc>()

        for (match in matches) {
            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { Acc() }
                acc.games++
                acc.totalInhibitorKills += p.inhibitorKills
                if (p.firstInhibitorKill || p.firstInhibitorAssist) acc.firstInhibitorGames++
                acc.totalChampLevel += p.champLevel
                acc.totalLongestTimeSpentLiving += p.longestTimeSpentLiving
                acc.totalLargestKillingSpree += p.largestKillingSpree
                acc.totalLargestMultiKill += p.largestMultiKill
            }
        }

        val players = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                val avgInhibitor = r2(acc.totalInhibitorKills / g)
                val firstInhibitorRate = r2(acc.firstInhibitorGames / g)
                val avgLevel = r2(acc.totalChampLevel / g)
                val avgLiving = (acc.totalLongestTimeSpentLiving / acc.games).toInt()
                val avgSpree = r2(acc.totalLargestKillingSpree / g)
                val avgMulti = r2(acc.totalLargestMultiKill / g)

                val lateGameScore = r2(
                    avgInhibitor * 0.25 +
                    firstInhibitorRate * 0.15 +
                    (avgLevel / 18.0) * 0.20 +
                    (avgLiving / 600.0).coerceAtMost(1.0) * 0.15 +
                    (avgSpree / 10.0).coerceAtMost(1.0) * 0.15 +
                    (avgMulti / 5.0).coerceAtMost(1.0) * 0.10
                )

                LateGamePlayerEntry(
                    riotId = riotId,
                    games = acc.games,
                    avgInhibitorKills = avgInhibitor,
                    firstInhibitorRate = firstInhibitorRate,
                    avgChampLevel = avgLevel,
                    avgLongestTimeSpentLiving = avgLiving,
                    avgLargestKillingSpree = avgSpree,
                    avgLargestMultiKill = avgMulti,
                    lateGameScore = lateGameScore,
                )
            }
            .sortedByDescending { it.lateGameScore }

        LateGameResult(players = players)
    }
}
