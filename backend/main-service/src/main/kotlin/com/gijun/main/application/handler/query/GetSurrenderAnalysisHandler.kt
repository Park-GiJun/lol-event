package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.SurrenderAnalysisResult
import com.gijun.main.application.dto.stats.result.SurrenderPlayerEntry
import com.gijun.main.application.port.`in`.GetSurrenderAnalysisUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetSurrenderAnalysisHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetSurrenderAnalysisUseCase {

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getSurrenderAnalysis(mode: String): SurrenderAnalysisResult = cache.getOrCompute("surrender-analysis:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

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

        val players = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                SurrenderPlayerEntry(
                    riotId = riotId,
                    games = acc.games,
                    surrenderGames = acc.surrenderGames,
                    earlySurrenderGames = acc.earlySurrenderGames,
                    causedEarlySurrenderGames = acc.causedEarlySurrenderGames,
                    surrenderRate = r2(acc.surrenderGames / g),
                    earlySurrenderRate = r2(acc.earlySurrenderGames / g),
                )
            }
            .sortedByDescending { it.surrenderRate }

        SurrenderAnalysisResult(
            totalGames = totalGames,
            surrenderGames = overallSurrenderGames,
            earlySurrenderGames = overallEarlySurrenderGames,
            overallSurrenderRate = if (totalGames > 0) r2(overallSurrenderGames.toDouble() / totalGames) else 0.0,
            overallEarlySurrenderRate = if (totalGames > 0) r2(overallEarlySurrenderGames.toDouble() / totalGames) else 0.0,
            players = players,
        )
    }
}
