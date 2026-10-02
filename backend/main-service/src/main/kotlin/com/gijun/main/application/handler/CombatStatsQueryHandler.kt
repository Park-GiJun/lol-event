package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.ChaosMatchEntry
import com.gijun.main.application.dto.result.ChaosMatchResult
import com.gijun.main.application.dto.result.DamageAnalysisResult
import com.gijun.main.application.dto.result.DamagePlayerEntry
import com.gijun.main.application.dto.result.LateGamePlayerEntry
import com.gijun.main.application.dto.result.LateGameResult
import com.gijun.main.application.dto.result.MultiKillEvent
import com.gijun.main.application.dto.result.MultiKillHighlightsResult
import com.gijun.main.application.dto.result.PlayerMultiKillStat
import com.gijun.main.application.dto.result.SurrenderAnalysisResult
import com.gijun.main.application.dto.result.SurrenderPlayerEntry
import com.gijun.main.application.port.`in`.GetChaosMatchUseCase
import com.gijun.main.application.port.`in`.GetDamageAnalysisUseCase
import com.gijun.main.application.port.`in`.GetLateGameUseCase
import com.gijun.main.application.port.`in`.GetMultiKillHighlightsUseCase
import com.gijun.main.application.port.`in`.GetSurrenderAnalysisUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 교전 통계 — 멀티킬 하이라이트, 난전 경기, 딜 분석, 항복 분석, 후반 지표.
 */
@Service
@Transactional(readOnly = true)
class CombatStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetMultiKillHighlightsUseCase,
    GetChaosMatchUseCase,
    GetDamageAnalysisUseCase,
    GetSurrenderAnalysisUseCase,
    GetLateGameUseCase {
    override fun getMultiKillHighlights(mode: GameMode): MultiKillHighlightsResult =
        statsResultCacheQueryPort.getOrCompute("multi-kill-highlights:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

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
                    acc.pentaKills += p.pentaKills
                    acc.quadraKills += p.quadraKills
                    acc.tripleKills += p.tripleKills
                    acc.doubleKills += p.doubleKills
                    acc.championCount[p.champion] = (acc.championCount[p.champion] ?: 0) + 1
                    acc.championIdMap[p.champion] = p.championId

                    // 펜타킬 이벤트 기록
                    repeat(p.pentaKills) {
                        allEvents.add(
                            MultiKillEvent(
                                riotId = p.riotId,
                                champion = p.champion,
                                championId = p.championId,
                                multiKillType = "PENTA",
                                matchId = match.matchId,
                                gameCreation = match.gameCreation,
                            ),
                        )
                    }
                    // 쿼드라킬 이벤트 기록
                    repeat(p.quadraKills) {
                        allEvents.add(
                            MultiKillEvent(
                                riotId = p.riotId,
                                champion = p.champion,
                                championId = p.championId,
                                multiKillType = "QUADRA",
                                matchId = match.matchId,
                                gameCreation = match.gameCreation,
                            ),
                        )
                    }
                    // 트리플킬 이벤트 기록
                    repeat(p.tripleKills) {
                        allEvents.add(
                            MultiKillEvent(
                                riotId = p.riotId,
                                champion = p.champion,
                                championId = p.championId,
                                multiKillType = "TRIPLE",
                                matchId = match.matchId,
                                gameCreation = match.gameCreation,
                            ),
                        )
                    }
                    // 더블킬 이벤트 기록
                    repeat(p.doubleKills) {
                        allEvents.add(
                            MultiKillEvent(
                                riotId = p.riotId,
                                champion = p.champion,
                                championId = p.championId,
                                multiKillType = "DOUBLE",
                                matchId = match.matchId,
                                gameCreation = match.gameCreation,
                            ),
                        )
                    }
                }
            }

            val pentaKillEvents =
                allEvents
                    .filter { it.multiKillType == "PENTA" }
                    .sortedByDescending { it.gameCreation }

            val recentHighlights =
                allEvents
                    .filter { it.multiKillType == "PENTA" || it.multiKillType == "QUADRA" }
                    .sortedByDescending { it.gameCreation }
                    .take(20)

            val playerRankings =
                accMap.entries
                    .filter { it.value.pentaKills + it.value.quadraKills + it.value.tripleKills + it.value.doubleKills > 0 }
                    .map { (riotId, acc) ->
                        val topChampion = acc.championCount.maxByOrNull { it.value }?.key
                        PlayerMultiKillStat(
                            riotId = riotId,
                            pentaKills = acc.pentaKills,
                            quadraKills = acc.quadraKills,
                            tripleKills = acc.tripleKills,
                            doubleKills = acc.doubleKills,
                            topChampion = topChampion,
                            topChampionId = topChampion?.let { acc.championIdMap[it] },
                        )
                    }.sortedWith(
                        compareByDescending<PlayerMultiKillStat> { it.pentaKills }
                            .thenByDescending { it.quadraKills }
                            .thenByDescending { it.tripleKills },
                    )

            MultiKillHighlightsResult(
                pentaKillEvents = pentaKillEvents,
                recentHighlights = recentHighlights,
                playerRankings = playerRankings,
            )
        }

    override fun getChaosMatch(mode: GameMode): ChaosMatchResult =
        statsResultCacheQueryPort.getOrCompute("chaos-match:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            if (matches.isEmpty()) {
                return@getOrCompute ChaosMatchResult(
                    topChaosMatches = emptyList(),
                    topBloodBathMatches = emptyList(),
                    topStrategicMatches = emptyList(),
                    avgChaosIndex = 0.0,
                )
            }

            // 1단계: 각 매치의 kill_density 계산 (평균 산출용)
            data class MatchStats(
                val matchId: String,
                val gameCreation: Long,
                val durationMin: Double,
                val totalKills: Int,
                val killDensity: Double,
                val multiKillScore: Int,
                val totalCcTime: Int,
                val hasSurrender: Boolean,
                val teamGolds: Map<Int, Int>,
                val participants: List<String>,
            )

            val matchStatsList =
                matches.map { match ->
                    val durationMin = maxOf(match.gameDuration / 60.0, 1.0)
                    val totalKills = match.participants.sumOf { it.kills }
                    val killDensity = totalKills / durationMin
                    val multiKillScore =
                        match.participants.sumOf { p ->
                            p.doubleKills * 1 + p.tripleKills * 3 + p.quadraKills * 6 + p.pentaKills * 10
                        }
                    val totalCcTime = match.participants.sumOf { it.timeCCingOthers }
                    val hasSurrender = match.participants.any { it.gameEndedInEarlySurrender }
                    val teamGolds =
                        match.participants
                            .groupBy { it.teamId }
                            .mapValues { (_, ps) -> ps.sumOf { it.gold } }
                    val participantIds = match.participants.map { it.riotId }

                    MatchStats(
                        matchId = match.matchId,
                        gameCreation = match.gameCreation,
                        durationMin = durationMin,
                        totalKills = totalKills,
                        killDensity = killDensity,
                        multiKillScore = multiKillScore,
                        totalCcTime = totalCcTime,
                        hasSurrender = hasSurrender,
                        teamGolds = teamGolds,
                        participants = participantIds,
                    )
                }

            val avgKillDensity = matchStatsList.map { it.killDensity }.average()

            val chaosEntries =
                matchStatsList.map { ms ->
                    val ccPerMin = ms.totalCcTime / ms.durationMin
                    val surrenderPenalty = if (ms.hasSurrender) 0.6 else 1.0
                    val chaosIndex =
                        (ms.killDensity * 3.0 + ms.multiKillScore * 2.0 + ccPerMin * 0.3) * surrenderPenalty

                    val goldValues = ms.teamGolds.values.toList()
                    val goldRatio =
                        if (goldValues.size >= 2) {
                            val minGold = goldValues.min()
                            val maxGold = goldValues.max()
                            minGold.toDouble() / maxOf(maxGold, 1)
                        } else {
                            1.0
                        }

                    val gameTypeTag =
                        when {
                            ms.killDensity > avgKillDensity && goldRatio > 0.88 -> "혈전"
                            ms.killDensity > avgKillDensity && goldRatio <= 0.75 -> "학살"
                            ms.killDensity <= avgKillDensity && goldRatio > 0.88 -> "운영 접전"
                            else -> "일반"
                        }

                    ChaosMatchEntry(
                        matchId = ms.matchId,
                        gameCreation = ms.gameCreation,
                        gameDurationMin = r2(ms.durationMin),
                        chaosIndex = r2(chaosIndex),
                        totalKills = ms.totalKills,
                        killDensity = r2(ms.killDensity),
                        multiKillScore = ms.multiKillScore,
                        gameTypeTag = gameTypeTag,
                        participants = ms.participants,
                    )
                }

            val topChaosMatches = chaosEntries.sortedByDescending { it.chaosIndex }.take(10)
            val topBloodBathMatches = chaosEntries.filter { it.gameTypeTag == "학살" }.sortedByDescending { it.chaosIndex }.take(5)
            val topStrategicMatches = chaosEntries.filter { it.gameTypeTag == "운영 접전" }.sortedByDescending { it.chaosIndex }.take(5)
            val avgChaosIndex = r2(chaosEntries.map { it.chaosIndex }.average())

            ChaosMatchResult(
                topChaosMatches = topChaosMatches,
                topBloodBathMatches = topBloodBathMatches,
                topStrategicMatches = topStrategicMatches,
                avgChaosIndex = avgChaosIndex,
            )
        }

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getDamageAnalysis(mode: GameMode): DamageAnalysisResult =
        statsResultCacheQueryPort.getOrCompute("damage-analysis:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class Acc(
                var games: Int = 0,
                var totalPhysical: Long = 0,
                var totalMagic: Long = 0,
                var totalTrue: Long = 0,
                var totalDmg: Long = 0,
                var totalMitigated: Long = 0,
                var totalTurretDmg: Long = 0,
            )

            val accMap = mutableMapOf<String, Acc>()

            for (match in matches) {
                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { Acc() }
                    acc.games++
                    acc.totalPhysical += p.physicalDamageDealtToChampions
                    acc.totalMagic += p.magicDamageDealtToChampions
                    acc.totalTrue += p.trueDamageDealtToChampions
                    acc.totalDmg += p.totalDamageDealtToChampions
                    acc.totalMitigated += p.damageSelfMitigated
                    acc.totalTurretDmg += p.damageDealtToTurrets
                }
            }

            val players =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        val g = acc.games
                        val avgPhysical = (acc.totalPhysical / g).toInt()
                        val avgMagic = (acc.totalMagic / g).toInt()
                        val avgTrue = (acc.totalTrue / g).toInt()
                        val avgTotal = (acc.totalDmg / g).toInt()
                        val total = (acc.totalPhysical + acc.totalMagic + acc.totalTrue).coerceAtLeast(1)
                        val physicalRate = r2(acc.totalPhysical.toDouble() / total)
                        val magicRate = r2(acc.totalMagic.toDouble() / total)
                        val trueRate = r2(acc.totalTrue.toDouble() / total)

                        val damageProfile =
                            when {
                                physicalRate >= 0.7 -> "AD"
                                magicRate >= 0.7 -> "AP"
                                acc.totalMitigated / g > avgTotal -> "Tank"
                                else -> "Hybrid"
                            }

                        DamagePlayerEntry(
                            riotId = riotId,
                            games = g,
                            avgPhysical = avgPhysical,
                            avgMagic = avgMagic,
                            avgTrue = avgTrue,
                            avgTotal = avgTotal,
                            avgMitigated = (acc.totalMitigated / g).toInt(),
                            avgTurretDmg = (acc.totalTurretDmg / g).toInt(),
                            physicalRate = physicalRate,
                            magicRate = magicRate,
                            trueRate = trueRate,
                            damageProfile = damageProfile,
                        )
                    }.sortedByDescending { it.avgTotal }

            DamageAnalysisResult(players = players)
        }

    override fun getSurrenderAnalysis(mode: GameMode): SurrenderAnalysisResult =
        statsResultCacheQueryPort.getOrCompute("surrender-analysis:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class Acc(
                var games: Int = 0,
                var surrenderGames: Int = 0,
                var earlySurrenderGames: Int = 0,
                var causedEarlySurrenderGames: Int = 0,
            )

            val accMap = mutableMapOf<String, Acc>()
            val matchIds = mutableSetOf<String>()
            var overallSurrenderGames = 0
            var overallEarlySurrenderGames = 0

            for (match in matches) {
                val isSurrender = match.participants.any { it.gameEndedInSurrender }
                val isEarlySurrender = match.participants.any { it.gameEndedInEarlySurrender }

                if (match.matchId !in matchIds) {
                    matchIds.add(match.matchId)
                    if (isSurrender) overallSurrenderGames++
                    if (isEarlySurrender) overallEarlySurrenderGames++
                }

                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { Acc() }
                    acc.games++
                    if (p.gameEndedInSurrender) acc.surrenderGames++
                    if (p.gameEndedInEarlySurrender) acc.earlySurrenderGames++
                    if (p.causedEarlySurrender) acc.causedEarlySurrenderGames++
                }
            }

            val totalGames = matchIds.size

            val players =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        val g = acc.games.toDouble()
                        SurrenderPlayerEntry(
                            riotId = riotId,
                            games = acc.games,
                            surrenderGames = acc.surrenderGames,
                            surrenderRate = r2(acc.surrenderGames / g),
                        )
                    }.sortedByDescending { it.surrenderRate }

            SurrenderAnalysisResult(
                totalGames = totalGames,
                surrenderGames = overallSurrenderGames,
                overallSurrenderRate = if (totalGames > 0) r2(overallSurrenderGames.toDouble() / totalGames) else 0.0,
                players = players,
            )
        }

    override fun getLateGame(mode: GameMode): LateGameResult =
        statsResultCacheQueryPort.getOrCompute("late-game:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

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

            val players =
                accMap.entries
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
                        val lateGameScore =
                            r2(
                                avgInhibitor * 0.29 +
                                    (avgLevel / 18.0) * 0.23 +
                                    (avgLiving / 600.0).coerceAtMost(1.0) * 0.18 +
                                    (avgSpree / 10.0).coerceAtMost(1.0) * 0.18 +
                                    (avgMulti / 5.0).coerceAtMost(1.0) * 0.12,
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
                    }.sortedByDescending { it.lateGameScore }

            LateGameResult(players = players)
        }
}
