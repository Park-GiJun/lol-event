package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.DefeatContributionEntry
import com.gijun.main.application.dto.result.DefeatContributionResult
import com.gijun.main.application.dto.result.PlayerPositionEntry
import com.gijun.main.application.dto.result.PlaystyleDnaEntry
import com.gijun.main.application.dto.result.PlaystyleDnaResult
import com.gijun.main.application.dto.result.PositionBadgeEntry
import com.gijun.main.application.dto.result.PositionBadgeResult
import com.gijun.main.application.dto.result.PositionChampEntry
import com.gijun.main.application.dto.result.PositionChampionPoolResult
import com.gijun.main.application.dto.result.SurvivalIndexEntry
import com.gijun.main.application.dto.result.SurvivalIndexResult
import com.gijun.main.application.port.`in`.GetDefeatContributionUseCase
import com.gijun.main.application.port.`in`.GetPlaystyleDnaUseCase
import com.gijun.main.application.port.`in`.GetPositionBadgeUseCase
import com.gijun.main.application.port.`in`.GetPositionChampionPoolUseCase
import com.gijun.main.application.port.`in`.GetSurvivalIndexUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.service.PositionResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 플레이 성향 통계 — 성향 DNA, 포지션 배지, 포지션별 챔피언 풀, 생존 지수, 패배 기여.
 */
@Service
@Transactional(readOnly = true)
class PlaystyleStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetPlaystyleDnaUseCase,
    GetPositionBadgeUseCase,
    GetPositionChampionPoolUseCase,
    GetSurvivalIndexUseCase,
    GetDefeatContributionUseCase {
    private fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getPlaystyleDna(mode: GameMode): PlaystyleDnaResult =
        statsResultCacheQueryPort.getOrCompute("playstyle-dna:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class PlayerRaw(
                val riotId: String,
                val games: Int,
                val aggression: Double,
                val durability: Double,
                val teamPlay: Double,
                val objectiveFocus: Double,
                val economy: Double,
                val visionControl: Double,
            )

            data class MatchParticipantStats(
                val kills: Int,
                val deaths: Int,
                val assists: Int,
                val doubleKills: Int,
                val tripleKills: Int,
                val pentaKills: Int,
                val longestTimeSpentLiving: Int,
                val damage: Int,
                val damageDealtToObjectives: Int,
                val damageDealtToTurrets: Int,
                val cs: Int,
                val gold: Int,
                val visionScore: Int,
                val wardsPlaced: Int,
                val wardsKilled: Int,
                val gameDuration: Int,
                val teamKills: Int,
            )

            // Collect per-match per-participant stats
            val allStats = mutableListOf<Pair<String, MatchParticipantStats>>()

            for (match in matches) {
                val durationSec = match.gameDuration

                for (teamId in listOf(100, 200)) {
                    val teamParticipants = match.participants.filter { it.teamId == teamId }
                    val teamKills = teamParticipants.sumOf { it.kills }

                    for (p in teamParticipants) {
                        allStats.add(
                            p.riotId to
                                MatchParticipantStats(
                                    kills = p.kills,
                                    deaths = p.deaths,
                                    assists = p.assists,
                                    doubleKills = p.doubleKills,
                                    tripleKills = p.tripleKills,
                                    pentaKills = p.pentaKills,
                                    longestTimeSpentLiving = p.longestTimeSpentLiving,
                                    damage = p.damage,
                                    damageDealtToObjectives = p.damageDealtToObjectives,
                                    damageDealtToTurrets = p.damageDealtToTurrets,
                                    cs = p.cs,
                                    gold = p.gold,
                                    visionScore = p.visionScore,
                                    wardsPlaced = p.wardsPlaced,
                                    wardsKilled = p.wardsKilled,
                                    gameDuration = durationSec,
                                    teamKills = teamKills,
                                ),
                        )
                    }
                }
            }

            // Compute raw scores per player
            val rawByPlayer =
                allStats.groupBy { it.first }.map { (riotId, entries) ->
                    val g = entries.size
                    val stats = entries.map { it.second }

                    val killsAvg = stats.map { it.kills }.average()
                    val deathsAvg = stats.map { it.deaths }.average()
                    val assistsAvg = stats.map { it.assists }.average()
                    val doubleAvg = stats.map { it.doubleKills }.average()
                    val tripleAvg = stats.map { it.tripleKills }.average()
                    val pentaAvg = stats.map { it.pentaKills }.average()
                    val longestLivingAvg = stats.map { it.longestTimeSpentLiving }.average()
                    val durationAvg = stats.map { it.gameDuration }.average()
                    val durationMinAvg = durationAvg / 60.0
                    val kpAvg =
                        stats
                            .map { s ->
                                val tk = s.teamKills
                                if (tk > 0) (s.kills + s.assists).toDouble() / tk else 0.0
                            }.average()
                    val objAvg = stats.map { it.damageDealtToObjectives }.average()
                    val turretAvg = stats.map { it.damageDealtToTurrets }.average()
                    val csAvg = stats.map { it.cs }.average()
                    val goldAvg = stats.map { it.gold }.average()
                    val visionAvg = stats.map { it.visionScore }.average()
                    val wardsPlacedAvg = stats.map { it.wardsPlaced }.average()
                    val wardsKilledAvg = stats.map { it.wardsKilled }.average()

                    val aggressionRaw = killsAvg * 3 + doubleAvg * 5 + tripleAvg * 8 + pentaAvg * 20
                    val durabilityRaw = longestLivingAvg / maxOf(1.0, durationAvg) * 40 + (1.0 / maxOf(1.0, deathsAvg)) * 20
                    val teamPlayRaw = assistsAvg * 2 + kpAvg * 30
                    val objectiveFocusRaw = objAvg * 0.01 + turretAvg * 0.02
                    val economyRaw = (csAvg / maxOf(1.0, durationMinAvg)) * 10 + goldAvg * 0.01
                    val visionControlRaw = visionAvg / maxOf(1.0, durationMinAvg) * 20 + wardsPlacedAvg * 2 + wardsKilledAvg * 3

                    PlayerRaw(riotId, g, aggressionRaw, durabilityRaw, teamPlayRaw, objectiveFocusRaw, economyRaw, visionControlRaw)
                }

            if (rawByPlayer.isEmpty()) return@getOrCompute PlaystyleDnaResult(players = emptyList())

            // Min-max normalization per axis
            fun normalize(values: List<Double>): List<Double> {
                val min = values.minOrNull() ?: 0.0
                val max = values.maxOrNull() ?: 0.0
                return if (max == min) {
                    values.map { 50.0 }
                } else {
                    values.map { (it - min) / (max - min) * 100.0 }
                }
            }

            val aggressionNorm = normalize(rawByPlayer.map { it.aggression })
            val durabilityNorm = normalize(rawByPlayer.map { it.durability })
            val teamPlayNorm = normalize(rawByPlayer.map { it.teamPlay })
            val objectiveFocusNorm = normalize(rawByPlayer.map { it.objectiveFocus })
            val economyNorm = normalize(rawByPlayer.map { it.economy })
            val visionControlNorm = normalize(rawByPlayer.map { it.visionControl })

            val players =
                rawByPlayer.indices
                    .map { i ->
                        val raw = rawByPlayer[i]
                        val agg = r2(aggressionNorm[i])
                        val dur = r2(durabilityNorm[i])
                        val tp = r2(teamPlayNorm[i])
                        val obj = r2(objectiveFocusNorm[i])
                        val eco = r2(economyNorm[i])
                        val vis = r2(visionControlNorm[i])

                        val styleTag =
                            when {
                                agg > 70 -> "킬몰이형"
                                tp > 70 && agg < 40 -> "팀빌더형"
                                obj > 70 -> "오브젝트 사냥꾼"
                                dur > 70 && tp > 60 -> "탱커 리더형"
                                eco > 70 && obj > 60 -> "운영형"
                                vis > 70 -> "정보 지배자"
                                else -> "올라운더"
                            }

                        PlaystyleDnaEntry(
                            riotId = raw.riotId,
                            games = raw.games,
                            aggression = agg,
                            durability = dur,
                            teamPlay = tp,
                            objectiveFocus = obj,
                            economy = eco,
                            visionControl = vis,
                            styleTag = styleTag,
                        )
                    }.sortedBy { it.riotId }

            PlaystyleDnaResult(players = players)
        }

    override fun getPositionBadge(mode: GameMode): PositionBadgeResult =
        statsResultCacheQueryPort.getOrCompute("position-badge:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class ParticipantRecord(
                val riotId: String,
                val champion: String,
                val championId: Int,
                val win: Boolean,
                val kills: Int,
                val deaths: Int,
                val assists: Int,
                val damage: Int,
                val visionScore: Int,
                val gameDuration: Int,
            )

            val positions = listOf("TOP", "JUNGLE", "MID", "BOTTOM", "SUPPORT")
            val positionData = positions.associateWith { mutableListOf<ParticipantRecord>() }

            for (match in matches) {
                for (p in match.participants) {
                    val pos = PositionResolver.resolve(p) ?: continue
                    positionData[pos]?.add(
                        ParticipantRecord(
                            riotId = p.riotId,
                            champion = p.champion,
                            championId = p.championId,
                            win = p.win,
                            kills = p.kills,
                            deaths = p.deaths,
                            assists = p.assists,
                            damage = p.damage,
                            visionScore = p.visionScore,
                            gameDuration = match.gameDuration,
                        ),
                    )
                }
            }

            val allPositionRankings = mutableMapOf<String, List<PositionBadgeEntry>>()

            for (pos in positions) {
                val records = positionData[pos] ?: emptyList()
                val avgDamageOverall = if (records.isNotEmpty()) records.map { it.damage }.average() else 1.0

                val playerEntries =
                    records
                        .groupBy { it.riotId }
                        .mapNotNull { (riotId, es) ->
                            if (es.size < 3) return@mapNotNull null
                            val g = es.size
                            val w = es.count { it.win }
                            val winRate = w.toDouble() / g
                            val k = es.sumOf { it.kills }
                            val d = es.sumOf { it.deaths }
                            val a = es.sumOf { it.assists }
                            val kda = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()
                            val avgDamage = es.map { it.damage }.average()
                            val avgVisionPerMin =
                                es
                                    .map { rec ->
                                        val durationMin = rec.gameDuration / 60.0
                                        rec.visionScore / maxOf(1.0, durationMin)
                                    }.average()

                            val damageShare = avgDamage / maxOf(1.0, avgDamageOverall)
                            val positionScore = winRate * 0.3 + kda * 0.25 + damageShare * 0.25 + avgVisionPerMin * 0.2

                            val topChampEntry = es.groupBy { it.championId }.maxByOrNull { it.value.size }

                            PositionBadgeEntry(
                                position = pos,
                                riotId = riotId,
                                games = g,
                                winRate = r2(winRate * 100),
                                kda = kda,
                                avgDamage = r2(avgDamage),
                                positionScore = r2(positionScore),
                                topChampion = topChampEntry?.value?.first()?.champion,
                                topChampionId = topChampEntry?.key,
                            )
                        }.sortedByDescending { it.positionScore }

                allPositionRankings[pos] = playerEntries
            }

            val topPositions =
                positions.mapNotNull { pos ->
                    allPositionRankings[pos]?.firstOrNull()
                }

            PositionBadgeResult(
                topPositions = topPositions,
                allPositionRankings = allPositionRankings,
            )
        }

    override fun getPositionChampionPool(mode: GameMode): PositionChampionPoolResult =
        statsResultCacheQueryPort.getOrCompute("position-champion-pool:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class ChampAcc(
                var games: Int = 0,
                var wins: Int = 0,
                var kills: Int = 0,
                var deaths: Int = 0,
                var assists: Int = 0,
                var championId: Int = 0,
            )

            // key: riotId + "|" + position
            val posAccMap = mutableMapOf<String, MutableMap<String, ChampAcc>>() // riotId|position -> champion -> acc

            for (match in matches) {
                for (p in match.participants) {
                    val pos = PositionResolver.resolve(p) ?: "UNKNOWN"
                    val key = "${p.riotId}|$pos"
                    val champMap = posAccMap.getOrPut(key) { mutableMapOf() }
                    val acc = champMap.getOrPut(p.champion) { ChampAcc().also { it.championId = p.championId } }
                    acc.games++
                    acc.championId = p.championId
                    if (p.win) acc.wins++
                    acc.kills += p.kills
                    acc.deaths += p.deaths
                    acc.assists += p.assists
                }
            }

            val allPlayers =
                posAccMap.entries
                    .filter { it.value.values.sumOf { a -> a.games } >= 3 }
                    .map { (key, champAcc) ->
                        val (riotId, position) = key.split("|", limit = 2)
                        val totalGames = champAcc.values.sumOf { it.games }
                        val totalWins = champAcc.values.sumOf { it.wins }

                        val champions =
                            champAcc.entries
                                .map { (champ, acc) ->
                                    val kda =
                                        if (acc.deaths == 0) {
                                            (acc.kills + acc.assists).toDouble()
                                        } else {
                                            (acc.kills + acc.assists).toDouble() / acc.deaths
                                        }
                                    PositionChampEntry(
                                        champion = champ,
                                        championId = acc.championId,
                                        games = acc.games,
                                        winRate = if (acc.games > 0) (acc.wins.toDouble() / acc.games * 1000).toInt() / 10.0 else 0.0,
                                        kda = (kda * 100).toInt() / 100.0,
                                    )
                                }.sortedByDescending { it.games }

                        val top = champions.firstOrNull()

                        PlayerPositionEntry(
                            riotId = riotId,
                            position = position,
                            games = totalGames,
                            winRate = if (totalGames > 0) (totalWins.toDouble() / totalGames * 1000).toInt() / 10.0 else 0.0,
                            topChampion = top?.champion,
                            topChampionId = top?.championId,
                            champions = champions.take(10),
                        )
                    }.sortedWith(compareBy({ it.riotId }, { it.position }))

            PositionChampionPoolResult(allPlayers = allPlayers)
        }

    fun r2Survival(v: Double) = (v * 100).toInt() / 100.0

    override fun getSurvivalIndex(mode: GameMode): SurvivalIndexResult =
        statsResultCacheQueryPort.getOrCompute("survival-index:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class PlayerAcc(
                var games: Int = 0,
                var totalDamageTaken: Double = 0.0,
                var totalSelfMitigated: Double = 0.0,
                var totalMitigationRatio: Double = 0.0,
                var totalTankShare: Double = 0.0,
                var totalSurvivalRatio: Double = 0.0,
                var totalDeaths: Double = 0.0,
            )

            val accMap = mutableMapOf<String, PlayerAcc>()

            for (match in matches) {
                val byTeam = match.participants.groupBy { it.teamId }
                val teamDamageTaken = byTeam.mapValues { (_, ps) -> ps.sumOf { it.totalDamageTaken } }

                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                    val teamTotalDamageTaken = teamDamageTaken[p.teamId] ?: 1

                    val mitigationRatio = p.damageSelfMitigated.toDouble() / maxOf(1, p.totalDamageTaken)
                    val tankShare = p.totalDamageTaken.toDouble() / maxOf(1, teamTotalDamageTaken)
                    val survivalRatio = p.longestTimeSpentLiving.toDouble() / maxOf(1, match.gameDuration)

                    acc.games++
                    acc.totalDamageTaken += p.totalDamageTaken
                    acc.totalSelfMitigated += p.damageSelfMitigated
                    acc.totalMitigationRatio += mitigationRatio
                    acc.totalTankShare += tankShare
                    acc.totalSurvivalRatio += survivalRatio
                    acc.totalDeaths += p.deaths
                }
            }

            val rankings =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        val g = acc.games.toDouble()
                        val avgMitigationRatio = acc.totalMitigationRatio / g
                        val avgTankShare = acc.totalTankShare / g
                        val avgSurvivalRatio = acc.totalSurvivalRatio / g
                        val avgDeaths = acc.totalDeaths / g

                        val survivalIndex =
                            avgMitigationRatio * 0.35 +
                                avgTankShare * 0.30 +
                                avgSurvivalRatio * 0.20 +
                                (1.0 / maxOf(1.0, avgDeaths)) * 0.15

                        SurvivalIndexEntry(
                            riotId = riotId,
                            games = acc.games,
                            avgDamageTaken = r2Survival(acc.totalDamageTaken / g),
                            avgSelfMitigated = r2Survival(acc.totalSelfMitigated / g),
                            avgMitigationRatio = r2Survival(avgMitigationRatio),
                            avgTankShare = r2Survival(avgTankShare),
                            avgSurvivalRatio = r2Survival(avgSurvivalRatio),
                            avgDeaths = r2Survival(avgDeaths),
                            survivalIndex = r2Survival(survivalIndex),
                        )
                    }.sortedByDescending { it.survivalIndex }

            SurvivalIndexResult(rankings = rankings)
        }

    override fun getDefeatContribution(mode: GameMode): DefeatContributionResult =
        statsResultCacheQueryPort.getOrCompute("defeat-contribution:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            data class PlayerAcc(
                var totalGames: Int = 0,
                var lossGames: Int = 0,
                var totalDefeatScore: Double = 0.0,
                var totalDeaths: Int = 0,
                var totalDamage: Int = 0,
                var worstMatchId: String? = null,
                var worstDefeatScore: Double = Double.MIN_VALUE,
            )

            val accMap = mutableMapOf<String, PlayerAcc>()

            for (match in matches) {
                val byTeam = match.participants.groupBy { it.teamId }

                // 팀별 평균 deaths, damage, cs, gold 계산
                val teamAvgDeaths = byTeam.mapValues { (_, ps) -> ps.map { it.deaths }.average() }
                val teamAvgDamage = byTeam.mapValues { (_, ps) -> ps.map { it.damage }.average() }
                val teamAvgCs = byTeam.mapValues { (_, ps) -> ps.map { it.cs }.average() }
                val teamAvgGold = byTeam.mapValues { (_, ps) -> ps.map { it.gold }.average() }

                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                    acc.totalGames++

                    if (!p.win) {
                        acc.lossGames++
                        acc.totalDeaths += p.deaths
                        acc.totalDamage += p.damage

                        val avgDeaths = teamAvgDeaths[p.teamId] ?: 1.0
                        val avgDamage = teamAvgDamage[p.teamId] ?: 1.0
                        val avgCs = teamAvgCs[p.teamId] ?: 1.0
                        val avgGold = teamAvgGold[p.teamId] ?: 1.0

                        val deathsScore = p.deaths / maxOf(avgDeaths, 1.0) * 3.0
                        val damageDef = (avgDamage - p.damage) / maxOf(avgDamage, 1.0) * 20.0
                        val csDef = (avgCs - p.cs) / maxOf(avgCs, 1.0) * 10.0
                        val goldDef = (avgGold - p.gold) / maxOf(avgGold, 1.0) * 10.0

                        val defeatScore =
                            deathsScore +
                                maxOf(0.0, damageDef) +
                                maxOf(0.0, csDef) +
                                maxOf(0.0, goldDef)

                        acc.totalDefeatScore += defeatScore
                        if (defeatScore > acc.worstDefeatScore) {
                            acc.worstDefeatScore = defeatScore
                            acc.worstMatchId = match.matchId
                        }
                    }
                }
            }

            val rankings =
                accMap.entries
                    .filter { it.value.lossGames >= 3 }
                    .map { (riotId, acc) ->
                        DefeatContributionEntry(
                            riotId = riotId,
                            games = acc.totalGames,
                            losses = acc.lossGames,
                            avgDefeatScore = r2(acc.totalDefeatScore / acc.lossGames),
                            avgDeaths = r2(acc.totalDeaths.toDouble() / acc.lossGames),
                            avgDamage = r2(acc.totalDamage.toDouble() / acc.lossGames),
                            worstMatch = acc.worstMatchId,
                        )
                    }.sortedByDescending { it.avgDefeatScore }

            DefeatContributionResult(rankings = rankings)
        }
}
