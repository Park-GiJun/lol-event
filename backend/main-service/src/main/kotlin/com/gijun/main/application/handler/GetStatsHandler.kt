package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.ChampionCount
import com.gijun.main.application.dto.result.PlayerStatsResult
import com.gijun.main.application.dto.result.StatsResult
import com.gijun.main.application.port.`in`.GetStatsUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.application.port.out.persistence.StatsCacheQueryPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetStatsHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val memberQueryPersistencePort: MemberQueryPersistencePort,
    private val statsCacheQueryPersistencePort: StatsCacheQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetStatsUseCase {
    override fun getStats(mode: String): StatsResult =
        statsResultCacheQueryPort.getOrCompute("stats:$mode") {
            val queueIds = modeToQueueIds(mode)
            val matchCount = matchQueryPersistencePort.countByQueueIds(queueIds)

            // 배치가 집계한 스냅샷이 있으면 캐시 우선 사용 (중복 집계 없음)
            val cached = statsCacheQueryPersistencePort.findPlayerCacheByMode(mode)
            if (cached.isNotEmpty()) {
                val stats =
                    cached
                        .sortedWith(
                            compareByDescending<com.gijun.main.application.port.out.persistence.PlayerStatsCache> { it.winRate }
                                .thenByDescending { it.games },
                        ).map { c ->
                            PlayerStatsResult(
                                riotId = c.riotId,
                                games = c.games,
                                wins = c.wins,
                                losses = c.losses,
                                winRate = c.winRate,
                                avgKills = c.avgKills,
                                avgDeaths = c.avgDeaths,
                                avgAssists = c.avgAssists,
                                kda = c.kda,
                                avgDamage = c.avgDamage,
                                avgCs = c.avgCs,
                                avgGold = c.avgGold,
                                avgVisionScore = c.avgVisionScore,
                                topChampions = listOfNotNull(c.topChampion?.let { ChampionCount(it, 0) }),
                            )
                        }
                return@getOrCompute StatsResult(stats, matchCount)
            }

            // 캐시 없음 → 원본 계산 (배치 미실행 초기 상태)
            val members = memberQueryPersistencePort.findAll()
            val matches = matchQueryPersistencePort.findAllWithParticipants(queueIds)

            data class Acc(
                val riotId: String,
                var wins: Int = 0,
                var losses: Int = 0,
                var kills: Int = 0,
                var deaths: Int = 0,
                var assists: Int = 0,
                var damage: Int = 0,
                var cs: Int = 0,
                var gold: Int = 0,
                var visionScore: Int = 0,
                var games: Int = 0,
                val champions: MutableMap<String, Int> = mutableMapOf(),
            )

            val accByPuuid = members.associate { it.puuid to Acc(it.riotId) }.toMutableMap()
            val accByRiotId = members.associate { it.riotId to accByPuuid.getValue(it.puuid) }

            for (match in matches) {
                for (p in match.participants) {
                    val s = (p.puuid?.let { accByPuuid[it] }) ?: accByRiotId[p.riotId] ?: continue
                    s.games++
                    if (p.win) s.wins++ else s.losses++
                    s.kills += p.kills
                    s.deaths += p.deaths
                    s.assists += p.assists
                    s.damage += p.damage
                    s.cs += p.cs
                    s.gold += p.gold
                    s.visionScore += p.visionScore
                    s.champions[p.champion] = (s.champions[p.champion] ?: 0) + 1
                }
            }

            val stats =
                accByPuuid.values
                    .filter { it.games > 0 }
                    .map { s ->
                        PlayerStatsResult(
                            riotId = s.riotId,
                            games = s.games,
                            wins = s.wins,
                            losses = s.losses,
                            winRate = (s.wins * 100 / s.games),
                            avgKills = (s.kills.toDouble() / s.games * 10).toInt() / 10.0,
                            avgDeaths = (s.deaths.toDouble() / s.games * 10).toInt() / 10.0,
                            avgAssists = (s.assists.toDouble() / s.games * 10).toInt() / 10.0,
                            kda =
                                if (s.deaths > 0) {
                                    ((s.kills + s.assists).toDouble() / s.deaths * 100).toInt() / 100.0
                                } else {
                                    (s.kills + s.assists).toDouble()
                                },
                            avgDamage = s.damage / s.games,
                            avgCs = (s.cs.toDouble() / s.games * 10).toInt() / 10.0,
                            avgGold = s.gold / s.games,
                            avgVisionScore = (s.visionScore.toDouble() / s.games * 10).toInt() / 10.0,
                            topChampions =
                                s.champions.entries
                                    .sortedByDescending { it.value }
                                    .take(3)
                                    .map { ChampionCount(it.key, it.value) },
                        )
                    }.sortedWith(compareByDescending<PlayerStatsResult> { it.winRate }.thenByDescending { it.games })

            StatsResult(stats, matchCount)
        }
}
