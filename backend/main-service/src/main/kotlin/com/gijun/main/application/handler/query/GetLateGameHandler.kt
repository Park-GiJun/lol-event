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
                val avgLevel = r2(acc.totalChampLevel / g)
                val avgLiving = (acc.totalLongestTimeSpentLiving / acc.games).toInt()
                val avgSpree = r2(acc.totalLargestKillingSpree / g)
                val avgMulti = r2(acc.totalLargestMultiKill / g)

                // 첫 억제기 비율(0.15)은 뺐다. first_inhibitor_kill / first_inhibitor_assist 가
                // 수집분 1,716행 전부 false 라 항상 0 이었고, 그만큼 점수가 눌려 있었다.
                // 남은 항에 비중을 나눠 다시 1.0 을 채운다. 상수 0 이 빠지는 것이라 순위는 그대로다.
                val lateGameScore = r2(
                    avgInhibitor * 0.29 +
                    (avgLevel / 18.0) * 0.23 +
                    (avgLiving / 600.0).coerceAtMost(1.0) * 0.18 +
                    (avgSpree / 10.0).coerceAtMost(1.0) * 0.18 +
                    (avgMulti / 5.0).coerceAtMost(1.0) * 0.12
                )

                LateGamePlayerEntry(
                    riotId = riotId,
                    games = acc.games,
                    avgInhibitorKills = avgInhibitor,
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
