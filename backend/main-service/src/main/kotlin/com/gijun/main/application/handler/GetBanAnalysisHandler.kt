package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.BanAnalysisResult
import com.gijun.main.application.dto.result.BanEntry
import com.gijun.main.application.port.`in`.GetBanAnalysisUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetBanAnalysisHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetBanAnalysisUseCase {
    override fun getBanAnalysis(mode: String): BanAnalysisResult =
        statsResultCacheQueryPort.getOrCompute("ban-analysis:$mode") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(modeToQueueIds(mode))
            if (matches.isEmpty()) return@getOrCompute BanAnalysisResult(emptyList(), 0, null)

            val totalGames = matches.size
            val banCountMap = mutableMapOf<Int, Pair<String, Int>>() // championId -> (name, count)

            for (match in matches) {
                for (team in match.teams) {
                    for (ban in team.bans) {
                        if (ban.championId <= 0) continue
                        val current = banCountMap[ban.championId]
                        banCountMap[ban.championId] = Pair(ban.championName, (current?.second ?: 0) + 1)
                    }
                }
            }

            val topBanned =
                banCountMap.entries
                    .map { (id, pair) ->
                        BanEntry(
                            champion = pair.first,
                            championId = id,
                            banCount = pair.second,
                            banRate = (pair.second.toDouble() / totalGames * 100).let { (it * 10).toInt() / 10.0 },
                        )
                    }.sortedByDescending { it.banCount }
                    .take(20)

            BanAnalysisResult(
                topBanned = topBanned,
                totalGamesAnalyzed = totalGames,
                mostBannedChampion = topBanned.firstOrNull()?.champion,
            )
        }
}
