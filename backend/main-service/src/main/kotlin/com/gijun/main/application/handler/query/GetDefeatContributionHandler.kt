package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.DefeatContributionEntry
import com.gijun.main.application.dto.stats.result.DefeatContributionResult
import com.gijun.main.application.port.`in`.GetDefeatContributionUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetDefeatContributionHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetDefeatContributionUseCase {
    override fun getDefeatContribution(mode: String): DefeatContributionResult = cache.getOrCompute("defeat-contribution:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

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
            val teamAvgCs     = byTeam.mapValues { (_, ps) -> ps.map { it.cs }.average() }
            val teamAvgGold   = byTeam.mapValues { (_, ps) -> ps.map { it.gold }.average() }

            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                acc.totalGames++

                if (!p.win) {
                    acc.lossGames++
                    acc.totalDeaths += p.deaths
                    acc.totalDamage += p.damage

                    val avgDeaths = teamAvgDeaths[p.teamId] ?: 1.0
                    val avgDamage = teamAvgDamage[p.teamId] ?: 1.0
                    val avgCs     = teamAvgCs[p.teamId] ?: 1.0
                    val avgGold   = teamAvgGold[p.teamId] ?: 1.0

                    val deathsScore   = p.deaths / maxOf(avgDeaths, 1.0) * 3.0
                    val damageDef     = (avgDamage - p.damage) / maxOf(avgDamage, 1.0) * 20.0
                    val csDef         = (avgCs - p.cs) / maxOf(avgCs, 1.0) * 10.0
                    val goldDef       = (avgGold - p.gold) / maxOf(avgGold, 1.0) * 10.0

                    val defeatScore = deathsScore +
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

        val rankings = accMap.entries
            .filter { it.value.lossGames >= 3 }
            .map { (riotId, acc) ->
                DefeatContributionEntry(
                    riotId         = riotId,
                    games          = acc.totalGames,
                    losses         = acc.lossGames,
                    avgDefeatScore = r2(acc.totalDefeatScore / acc.lossGames),
                    avgDeaths      = r2(acc.totalDeaths.toDouble() / acc.lossGames),
                    avgDamage      = r2(acc.totalDamage.toDouble() / acc.lossGames),
                    worstMatch     = acc.worstMatchId,
                )
            }
            .sortedByDescending { it.avgDefeatScore }

        DefeatContributionResult(rankings = rankings)
    }
}
