package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.GoldEfficiencyEntry
import com.gijun.main.application.dto.result.GoldEfficiencyResult
import com.gijun.main.application.dto.result.JungleDominanceEntry
import com.gijun.main.application.dto.result.JungleDominanceResult
import com.gijun.main.application.dto.result.SupportImpactEntry
import com.gijun.main.application.dto.result.SupportImpactResult
import com.gijun.main.application.dto.result.VisionDominanceResult
import com.gijun.main.application.dto.result.VisionPlayerEntry
import com.gijun.main.application.port.`in`.GetGoldEfficiencyUseCase
import com.gijun.main.application.port.`in`.GetJungleDominanceUseCase
import com.gijun.main.application.port.`in`.GetSupportImpactUseCase
import com.gijun.main.application.port.`in`.GetVisionDominanceUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 역할 수행 통계 — 정글 장악, 서포터 영향력, 시야 장악, 골드 효율.
 */
@Service
@Transactional(readOnly = true)
class RoleStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetJungleDominanceUseCase,
    GetSupportImpactUseCase,
    GetVisionDominanceUseCase,
    GetGoldEfficiencyUseCase {
    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getJungleDominance(mode: GameMode): JungleDominanceResult =
        statsResultCacheQueryPort.getOrCompute("jungle-dominance:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class PlayerAcc(
                var games: Int = 0,
                var totalInvadeRatio: Double = 0.0,
                var totalObjShare: Double = 0.0,
                var totalKp: Double = 0.0,
                var totalJungleCs: Double = 0.0,
                var totalVisionPerMin: Double = 0.0,
                val champions: MutableMap<String, Int> = mutableMapOf(),
                val championIds: MutableMap<String, Int> = mutableMapOf(),
            )

            val accMap = mutableMapOf<String, PlayerAcc>()

            for (match in matches) {
                val byTeam = match.participants.groupBy { it.teamId }
                val teamKills = byTeam.mapValues { (_, ps) -> ps.sumOf { it.kills } }
                val teamObjDamage = byTeam.mapValues { (_, ps) -> ps.sumOf { it.damageDealtToObjectives } }

                val durationMin = maxOf(1.0, match.gameDuration / 60.0)
                val junglers = match.participants.filter { it.neutralMinionsKilled >= 30 }

                for (p in junglers) {
                    val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                    val teamKillsTotal = teamKills[p.teamId] ?: 1
                    val teamObjTotal = teamObjDamage[p.teamId] ?: 1

                    val invadeRatio = p.neutralMinionsKilledEnemyJungle.toDouble() / maxOf(1, p.neutralMinionsKilled)
                    val objShare = p.damageDealtToObjectives.toDouble() / maxOf(1, teamObjTotal)
                    val kp = (p.kills + p.assists).toDouble() / maxOf(1, teamKillsTotal)
                    val visionPerMin = p.visionScore / durationMin

                    acc.games++
                    acc.totalInvadeRatio += invadeRatio
                    acc.totalObjShare += objShare
                    acc.totalKp += kp
                    acc.totalJungleCs += p.neutralMinionsKilled.toDouble()
                    acc.totalVisionPerMin += visionPerMin
                    acc.champions[p.champion] = (acc.champions[p.champion] ?: 0) + 1
                    acc.championIds[p.champion] = p.championId
                }
            }

            val rankings =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        val g = acc.games.toDouble()
                        val avgInvadeRatio = acc.totalInvadeRatio / g
                        val avgObjShare = acc.totalObjShare / g
                        val avgKp = acc.totalKp / g
                        val avgJungleCs = acc.totalJungleCs / g
                        val avgVisionPerMin = acc.totalVisionPerMin / g

                        // 카정 비율(avgInvadeRatio)은 빼 뒀다.
                        // neutral_minions_killed_enemy_jungle 이 수집분 1,716행 전부 0 이라 항상 0 이 나온다.
                        // 예전에는 이 항에 0.25 를 주고 있어서 점수가 통째로 4분의 3 스케일로 눌려 있었다.
                        // 남은 셋에 비중을 나눠 실제 0~1 범위를 되찾는다. 순위 자체는 예전과 같다
                        // (상수 0 이 빠지는 것이라 상대 순서는 안 바뀐다).
                        val jungleDominance =
                            avgObjShare * 0.40 +
                                avgKp * 0.40 +
                                avgVisionPerMin * 0.20

                        // 태그도 같은 이유로 다시 짰다. 예전 조건은
                        //   "공격형"      = 카정 > 0.15 → 절대 참이 안 됨. 죽은 가지였다.
                        //   "안전 갱킹형" = 킬관여 > 0.6 && 카정 < 0.1 → 뒤 조건이 항상 참이라 사실상 킬관여만 봤다.
                        // 카정 대신 정글 몹 수로 파밍형을 가른다. 기준값 195 는 실제 정글러 309표본의
                        // 상위 25% 경계다(중앙값 171, 최대 351).
                        val playStyleTag =
                            when {
                                avgObjShare > 0.35 -> "오브젝트 특화"
                                avgKp > 0.6 -> "갱킹형"
                                avgJungleCs >= FARMING_JUNGLE_CS -> "파밍형"
                                else -> "밸런스형"
                            }

                        val topChampion = acc.champions.maxByOrNull { it.value }?.key

                        JungleDominanceEntry(
                            riotId = riotId,
                            games = acc.games,
                            avgInvadeRatio = r2(avgInvadeRatio),
                            avgObjShare = r2(avgObjShare),
                            avgKp = r2(avgKp),
                            avgJungleCs = r2(avgJungleCs),
                            avgJungleDominance = r2(jungleDominance),
                            playStyleTag = playStyleTag,
                            topChampion = topChampion,
                            topChampionId = topChampion?.let { acc.championIds[it] },
                        )
                    }.sortedByDescending { it.avgJungleDominance }

            JungleDominanceResult(rankings = rankings)
        }

    override fun getSupportImpact(mode: GameMode): SupportImpactResult =
        statsResultCacheQueryPort.getOrCompute("support-impact:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class PlayerAcc(
                var games: Int = 0,
                var totalHealShare: Double = 0.0,
                var totalCcShare: Double = 0.0,
                var totalVisionShare: Double = 0.0,
                var totalShieldProxy: Double = 0.0,
                val champions: MutableMap<String, Int> = mutableMapOf(),
                val championIds: MutableMap<String, Int> = mutableMapOf(),
            )

            val accMap = mutableMapOf<String, PlayerAcc>()

            for (match in matches) {
                val byTeam = match.participants.groupBy { it.teamId }
                val teamHeal = byTeam.mapValues { (_, ps) -> ps.sumOf { it.totalHeal } }
                val teamCc = byTeam.mapValues { (_, ps) -> ps.sumOf { it.timeCCingOthers } }
                val teamVision = byTeam.mapValues { (_, ps) -> ps.sumOf { it.visionScore } }

                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }

                    val teamHealTotal = teamHeal[p.teamId] ?: 1
                    val teamCcTotal = teamCc[p.teamId] ?: 1
                    val teamVisionTotal = teamVision[p.teamId] ?: 1

                    val healShare = p.totalHeal.toDouble() / maxOf(1, teamHealTotal)
                    val ccShare = p.timeCCingOthers.toDouble() / maxOf(1, teamCcTotal)
                    val visionShare = p.visionScore.toDouble() / maxOf(1, teamVisionTotal)
                    val shieldProxy = p.damageSelfMitigated.toDouble() / maxOf(1, p.totalDamageTaken)

                    acc.games++
                    acc.totalHealShare += healShare
                    acc.totalCcShare += ccShare
                    acc.totalVisionShare += visionShare
                    acc.totalShieldProxy += shieldProxy
                    acc.champions[p.champion] = (acc.champions[p.champion] ?: 0) + 1
                    acc.championIds[p.champion] = p.championId
                }
            }

            val rankings =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        val g = acc.games.toDouble()
                        val avgHealShare = acc.totalHealShare / g
                        val avgCcShare = acc.totalCcShare / g
                        val avgVisionShare = acc.totalVisionShare / g
                        val avgShieldProxy = acc.totalShieldProxy / g

                        val supportImpact =
                            avgHealShare * 0.30 +
                                avgCcShare * 0.30 +
                                avgVisionShare * 0.25 +
                                avgShieldProxy * 0.15

                        val roleTag =
                            when {
                                avgHealShare > 0.4 -> "팀 힐러"
                                avgCcShare > 0.4 -> "CC 머신"
                                avgVisionShare > 0.4 -> "시야 지배자"
                                else -> "밸런스 서포터"
                            }

                        val topChampion = acc.champions.maxByOrNull { it.value }?.key

                        SupportImpactEntry(
                            riotId = riotId,
                            games = acc.games,
                            avgHealShare = r2(avgHealShare),
                            avgCcShare = r2(avgCcShare),
                            avgVisionShare = r2(avgVisionShare),
                            avgShieldProxy = r2(avgShieldProxy),
                            supportImpact = r2(supportImpact),
                            roleTag = roleTag,
                            topChampion = topChampion,
                            topChampionId = topChampion?.let { acc.championIds[it] },
                        )
                    }.sortedByDescending { it.supportImpact }

            SupportImpactResult(rankings = rankings)
        }

    override fun getVisionDominance(mode: GameMode): VisionDominanceResult =
        statsResultCacheQueryPort.getOrCompute("vision-dominance:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class Acc(
                var games: Int = 0,
                var totalVisionScore: Long = 0,
                var totalWardsPlaced: Long = 0,
                var totalWardsKilled: Long = 0,
                var totalSightWardsBought: Long = 0,
                var totalControlWardsBought: Long = 0,
            )

            val accMap = mutableMapOf<String, Acc>()

            for (match in matches) {
                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { Acc() }
                    acc.games++
                    acc.totalVisionScore += p.visionScore
                    acc.totalWardsPlaced += p.wardsPlaced
                    acc.totalWardsKilled += p.wardsKilled
                    acc.totalSightWardsBought += p.sightWardsBoughtInGame
                    acc.totalControlWardsBought += p.visionWardsBoughtInGame
                }
            }

            val players =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        val g = acc.games.toDouble()
                        val totalWards = (acc.totalWardsPlaced + acc.totalWardsKilled).coerceAtLeast(1)
                        VisionPlayerEntry(
                            riotId = riotId,
                            games = acc.games,
                            avgVisionScore = r2(acc.totalVisionScore / g),
                            avgWardsPlaced = r2(acc.totalWardsPlaced / g),
                            avgWardsKilled = r2(acc.totalWardsKilled / g),
                            avgControlWardsBought = r2(acc.totalControlWardsBought / g),
                            wardKillRate = r2(acc.totalWardsKilled.toDouble() / totalWards),
                        )
                    }.sortedByDescending { it.avgVisionScore }

            VisionDominanceResult(players = players)
        }

    fun r1(v: Double) = (v * 10).toInt() / 10.0

    override fun getGoldEfficiency(mode: GameMode): GoldEfficiencyResult =
        statsResultCacheQueryPort.getOrCompute("gold-efficiency:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class PlayerAcc(
                var games: Int = 0,
                var totalDmgPerGold: Double = 0.0,
                var totalVisionPerGold: Double = 0.0,
                var totalObjPerGold: Double = 0.0,
                var totalCsPerGold: Double = 0.0,
            )

            val accMap = mutableMapOf<String, PlayerAcc>()

            for (match in matches) {
                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                    val gold = maxOf(1, p.gold)
                    val goldK = maxOf(1.0, p.gold / 1000.0)

                    acc.games++
                    acc.totalDmgPerGold += p.damage.toDouble() / gold
                    acc.totalVisionPerGold += p.visionScore.toDouble() / goldK
                    acc.totalObjPerGold += p.damageDealtToObjectives.toDouble() / gold
                    acc.totalCsPerGold += p.cs.toDouble() * 1000.0 / gold
                }
            }

            data class RawEntry(
                val riotId: String,
                val games: Int,
                val avgDmgPerGold: Double,
                val avgVisionPerGold: Double,
                val avgObjPerGold: Double,
                val avgCsPerGold: Double,
            )

            val rawEntries =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        val g = acc.games.toDouble()
                        RawEntry(
                            riotId = riotId,
                            games = acc.games,
                            avgDmgPerGold = acc.totalDmgPerGold / g,
                            avgVisionPerGold = acc.totalVisionPerGold / g,
                            avgObjPerGold = acc.totalObjPerGold / g,
                            avgCsPerGold = acc.totalCsPerGold / g,
                        )
                    }

            if (rawEntries.isEmpty()) {
                return@getOrCompute GoldEfficiencyResult(
                    rankings = emptyList(),
                    dmgEfficiencyKing = null,
                    visionEfficiencyKing = null,
                    csEfficiencyKing = null,
                    objEfficiencyKing = null,
                )
            }

            // Min-max normalization helpers
            fun minMaxNorm(
                value: Double,
                min: Double,
                max: Double,
            ): Double = if (max == min) 0.5 else (value - min) / (max - min)

            val minDmg = rawEntries.minOf { it.avgDmgPerGold }
            val maxDmg = rawEntries.maxOf { it.avgDmgPerGold }
            val minVision = rawEntries.minOf { it.avgVisionPerGold }
            val maxVision = rawEntries.maxOf { it.avgVisionPerGold }
            val minObj = rawEntries.minOf { it.avgObjPerGold }
            val maxObj = rawEntries.maxOf { it.avgObjPerGold }
            val minCs = rawEntries.minOf { it.avgCsPerGold }
            val maxCs = rawEntries.maxOf { it.avgCsPerGold }

            val dmgEfficiencyKing = rawEntries.maxByOrNull { it.avgDmgPerGold }?.riotId
            val visionEfficiencyKing = rawEntries.maxByOrNull { it.avgVisionPerGold }?.riotId
            val csEfficiencyKing = rawEntries.maxByOrNull { it.avgCsPerGold }?.riotId
            val objEfficiencyKing = rawEntries.maxByOrNull { it.avgObjPerGold }?.riotId

            val rankings =
                rawEntries
                    .map { raw ->
                        val normDmg = minMaxNorm(raw.avgDmgPerGold, minDmg, maxDmg)
                        val normVision = minMaxNorm(raw.avgVisionPerGold, minVision, maxVision)
                        val normObj = minMaxNorm(raw.avgObjPerGold, minObj, maxObj)
                        val normCs = minMaxNorm(raw.avgCsPerGold, minCs, maxCs)

                        val goldEfficiencyScore = normDmg * 0.40 + normVision * 0.20 + normObj * 0.20 + normCs * 0.20

                        val tags = mutableListOf<String>()
                        if (raw.riotId == dmgEfficiencyKing) tags.add("딜러의 양심")
                        if (raw.riotId == visionEfficiencyKing) tags.add("와드의 신")
                        if (raw.riotId == csEfficiencyKing) tags.add("파밍장인")
                        if (raw.riotId == objEfficiencyKing) tags.add("오브젝트 효율왕")

                        GoldEfficiencyEntry(
                            riotId = raw.riotId,
                            games = raw.games,
                            avgDmgPerGold = r2(raw.avgDmgPerGold),
                            avgVisionPerGold = r2(raw.avgVisionPerGold),
                            avgObjPerGold = r2(raw.avgObjPerGold),
                            avgCsPerGold = r2(raw.avgCsPerGold),
                            goldEfficiencyScore = r2(goldEfficiencyScore),
                            tags = tags,
                        )
                    }.sortedByDescending { it.goldEfficiencyScore }

            GoldEfficiencyResult(
                rankings = rankings,
                dmgEfficiencyKing = dmgEfficiencyKing,
                visionEfficiencyKing = visionEfficiencyKing,
                csEfficiencyKing = csEfficiencyKing,
                objEfficiencyKing = objEfficiencyKing,
            )
        }

    companion object {
        /** 파밍형으로 가르는 평균 정글 몹 수. 정글러 표본 상위 25% 경계. */
        private const val FARMING_JUNGLE_CS = 195.0
    }
}
