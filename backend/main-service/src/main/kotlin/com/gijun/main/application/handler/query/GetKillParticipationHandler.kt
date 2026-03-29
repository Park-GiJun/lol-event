package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.KillParticipationEntry
import com.gijun.main.application.dto.stats.result.KillParticipationResult
import com.gijun.main.application.port.`in`.GetKillParticipationUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetKillParticipationHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
) : GetKillParticipationUseCase {

    override fun getKillParticipation(mode: String): KillParticipationResult = cache.getOrCompute("kill-participation:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayerAcc(
            var games: Int = 0,
            var totalKp: Double = 0.0,
            var winKp: Double = 0.0, var winGames: Int = 0,
            var lossKp: Double = 0.0, var lossGames: Int = 0,
            var totalKills: Int = 0,
            var totalAssists: Int = 0,
        )

        val accMap = mutableMapOf<String, PlayerAcc>()

        for (match in matches) {
            val teams = match.participants.groupBy { it.team }
            for ((_, teamParts) in teams) {
                val teamKills = maxOf(1, teamParts.sumOf { it.kills })
                for (p in teamParts) {
                    val kp = (p.kills + p.assists).toDouble() / teamKills
                    val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                    acc.games++
                    acc.totalKp += kp
                    acc.totalKills += p.kills
                    acc.totalAssists += p.assists
                    if (p.win) {
                        acc.winKp += kp
                        acc.winGames++
                    } else {
                        acc.lossKp += kp
                        acc.lossGames++
                    }
                }
            }
        }

        fun r2(v: Double) = (v * 10000).toInt() / 100.0

        val rankings = accMap.entries
            .filter { it.value.games >= 1 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                KillParticipationEntry(
                    riotId = riotId,
                    games = acc.games,
                    avgKp = r2(acc.totalKp / g),
                    avgKpWin = if (acc.winGames > 0) r2(acc.winKp / acc.winGames) else 0.0,
                    avgKpLoss = if (acc.lossGames > 0) r2(acc.lossKp / acc.lossGames) else 0.0,
                    avgKills = (acc.totalKills.toDouble() / g * 10).toInt() / 10.0,
                    avgAssists = (acc.totalAssists.toDouble() / g * 10).toInt() / 10.0,
                )
            }
            .sortedByDescending { it.avgKp }

        KillParticipationResult(
            rankings = rankings,
            kpKing = rankings.firstOrNull()?.riotId,
        )
    }
}
