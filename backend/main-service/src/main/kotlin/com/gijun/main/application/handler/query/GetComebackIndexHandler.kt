package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ComebackIndexEntry
import com.gijun.main.application.dto.stats.result.ComebackIndexResult
import com.gijun.main.application.dto.stats.result.ComebackMatchEntry
import com.gijun.main.application.port.`in`.GetComebackIndexUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetComebackIndexHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
) : GetComebackIndexUseCase {

    fun r1(v: Double) = (v * 10).toInt() / 10.0
    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getComebackIndex(mode: String): ComebackIndexResult = cache.getOrCompute("comeback-index:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayerAcc(
            var totalGames: Int = 0,
            var totalWins: Int = 0,
            var contestGames: Int = 0,
            var contestWins: Int = 0,
            var surrenderGames: Int = 0,
            var surrenderWins: Int = 0,
        )

        val accMap = mutableMapOf<String, PlayerAcc>()
        val contestMatchCandidates = mutableListOf<ComebackMatchEntry>()

        for (match in matches) {
            val isEarlySurrender = match.participants.any { it.gameEndedInEarlySurrender }
            val isContestMatch = !isEarlySurrender && match.gameDuration >= 1800

            if (isContestMatch) {
                val winners = match.participants.filter { it.win }.map { it.riotId }
                contestMatchCandidates.add(
                    ComebackMatchEntry(
                        matchId = match.matchId,
                        gameCreation = match.gameCreation,
                        gameDurationMin = r2(match.gameDuration / 60.0),
                        winnerParticipants = winners,
                    )
                )
            }

            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                acc.totalGames++
                if (p.win) acc.totalWins++

                if (isContestMatch) {
                    acc.contestGames++
                    if (p.win) acc.contestWins++
                }

                if (isEarlySurrender) {
                    acc.surrenderGames++
                    if (p.win) acc.surrenderWins++
                }
            }
        }

        // Top 5 contest matches by duration (longest = most likely comeback scenario)
        val topComebackMatches = contestMatchCandidates
            .sortedByDescending { it.gameDurationMin }
            .take(5)

        val entries = accMap.entries
            .filter { it.value.totalGames > 0 }
            .map { (riotId, acc) ->
                val totalWinRate = if (acc.totalGames > 0)
                    (acc.totalWins * 100.0 / acc.totalGames).toInt() else 0
                val contestWinRate = if (acc.contestGames > 0)
                    (acc.contestWins * 100.0 / acc.contestGames).toInt() else 0
                val surrenderWinRate = if (acc.surrenderGames > 0)
                    (acc.surrenderWins * 100.0 / acc.surrenderGames).toInt() else 0
                val comebackBonus = contestWinRate - totalWinRate
                val isKing = acc.contestGames >= 5 && comebackBonus >= 10

                ComebackIndexEntry(
                    riotId = riotId,
                    totalGames = acc.totalGames,
                    totalWinRate = totalWinRate,
                    contestGames = acc.contestGames,
                    contestWinRate = contestWinRate,
                    surrenderGames = acc.surrenderGames,
                    surrenderWinRate = surrenderWinRate,
                    comebackBonus = comebackBonus,
                    isKing = isKing,
                )
            }
            .sortedByDescending { it.comebackBonus }

        val comebackKing = entries
            .filter { it.isKing }
            .maxByOrNull { it.comebackBonus }
            ?.riotId

        ComebackIndexResult(
            rankings = entries,
            comebackKing = comebackKing,
            topComebackMatches = topComebackMatches,
        )
    }
}
