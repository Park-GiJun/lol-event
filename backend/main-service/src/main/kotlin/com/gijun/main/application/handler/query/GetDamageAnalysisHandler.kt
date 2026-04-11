package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.DamageAnalysisResult
import com.gijun.main.application.dto.stats.result.DamagePlayerEntry
import com.gijun.main.application.port.`in`.GetDamageAnalysisUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetDamageAnalysisHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetDamageAnalysisUseCase {

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getDamageAnalysis(mode: String): DamageAnalysisResult = cache.getOrCompute("damage-analysis:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

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

        val players = accMap.entries
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

                val damageProfile = when {
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
            }
            .sortedByDescending { it.avgTotal }

        DamageAnalysisResult(players = players)
    }
}
