package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.GoldEfficiencyEntry
import com.gijun.main.application.dto.stats.result.GoldEfficiencyResult
import com.gijun.main.application.port.`in`.GetGoldEfficiencyUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetGoldEfficiencyHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetGoldEfficiencyUseCase {

    fun r1(v: Double) = (v * 10).toInt() / 10.0
    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getGoldEfficiency(mode: String): GoldEfficiencyResult = cache.getOrCompute("gold-efficiency:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

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

        val rawEntries = accMap.entries
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
        fun minMaxNorm(value: Double, min: Double, max: Double): Double =
            if (max == min) 0.5 else (value - min) / (max - min)

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

        val rankings = rawEntries.map { raw ->
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
}
