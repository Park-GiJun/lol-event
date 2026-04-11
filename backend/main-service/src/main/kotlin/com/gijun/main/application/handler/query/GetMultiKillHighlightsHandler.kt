package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.MultiKillEvent
import com.gijun.main.application.dto.stats.result.MultiKillHighlightsResult
import com.gijun.main.application.dto.stats.result.PlayerMultiKillStat
import com.gijun.main.application.port.`in`.GetMultiKillHighlightsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetMultiKillHighlightsHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetMultiKillHighlightsUseCase {
    override fun getMultiKillHighlights(mode: String): MultiKillHighlightsResult = cache.getOrCompute("multi-kill-highlights:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        val allEvents = mutableListOf<MultiKillEvent>()

        data class PlayerAcc(
            var pentaKills: Int = 0,
            var quadraKills: Int = 0,
            var tripleKills: Int = 0,
            var doubleKills: Int = 0,
            val championCount: MutableMap<String, Int> = mutableMapOf(),
            val championIdMap: MutableMap<String, Int> = mutableMapOf(),
        )

        val accMap = mutableMapOf<String, PlayerAcc>()

        for (match in matches) {
            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                acc.pentaKills  += p.pentaKills
                acc.quadraKills += p.quadraKills
                acc.tripleKills += p.tripleKills
                acc.doubleKills += p.doubleKills
                acc.championCount[p.champion] = (acc.championCount[p.champion] ?: 0) + 1
                acc.championIdMap[p.champion] = p.championId

                // 펜타킬 이벤트 기록
                repeat(p.pentaKills) {
                    allEvents.add(
                        MultiKillEvent(
                            riotId        = p.riotId,
                            champion      = p.champion,
                            championId    = p.championId,
                            multiKillType = "PENTA",
                            matchId       = match.matchId,
                            gameCreation  = match.gameCreation,
                        )
                    )
                }
                // 쿼드라킬 이벤트 기록
                repeat(p.quadraKills) {
                    allEvents.add(
                        MultiKillEvent(
                            riotId        = p.riotId,
                            champion      = p.champion,
                            championId    = p.championId,
                            multiKillType = "QUADRA",
                            matchId       = match.matchId,
                            gameCreation  = match.gameCreation,
                        )
                    )
                }
                // 트리플킬 이벤트 기록
                repeat(p.tripleKills) {
                    allEvents.add(
                        MultiKillEvent(
                            riotId        = p.riotId,
                            champion      = p.champion,
                            championId    = p.championId,
                            multiKillType = "TRIPLE",
                            matchId       = match.matchId,
                            gameCreation  = match.gameCreation,
                        )
                    )
                }
                // 더블킬 이벤트 기록
                repeat(p.doubleKills) {
                    allEvents.add(
                        MultiKillEvent(
                            riotId        = p.riotId,
                            champion      = p.champion,
                            championId    = p.championId,
                            multiKillType = "DOUBLE",
                            matchId       = match.matchId,
                            gameCreation  = match.gameCreation,
                        )
                    )
                }
            }
        }

        val pentaKillEvents = allEvents
            .filter { it.multiKillType == "PENTA" }
            .sortedByDescending { it.gameCreation }

        val recentHighlights = allEvents
            .filter { it.multiKillType == "PENTA" || it.multiKillType == "QUADRA" }
            .sortedByDescending { it.gameCreation }
            .take(20)

        val playerRankings = accMap.entries
            .filter { it.value.pentaKills + it.value.quadraKills + it.value.tripleKills + it.value.doubleKills > 0 }
            .map { (riotId, acc) ->
                val topChampion = acc.championCount.maxByOrNull { it.value }?.key
                PlayerMultiKillStat(
                    riotId      = riotId,
                    pentaKills  = acc.pentaKills,
                    quadraKills = acc.quadraKills,
                    tripleKills = acc.tripleKills,
                    doubleKills = acc.doubleKills,
                    topChampion   = topChampion,
                    topChampionId = topChampion?.let { acc.championIdMap[it] },
                )
            }
            .sortedWith(
                compareByDescending<PlayerMultiKillStat> { it.pentaKills }
                    .thenByDescending { it.quadraKills }
                    .thenByDescending { it.tripleKills }
            )

        MultiKillHighlightsResult(
            pentaKillEvents  = pentaKillEvents,
            recentHighlights = recentHighlights,
            playerRankings   = playerRankings,
        )
    }
}
