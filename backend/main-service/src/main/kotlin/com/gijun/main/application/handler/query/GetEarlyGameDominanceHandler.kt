package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.EarlyGameDominanceEntry
import com.gijun.main.application.dto.stats.result.EarlyGameDominanceResult
import com.gijun.main.application.port.`in`.GetEarlyGameDominanceUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetEarlyGameDominanceHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetEarlyGameDominanceUseCase {

    fun r1(v: Double) = (v * 10).toInt() / 10.0
    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getEarlyGameDominance(mode: String): EarlyGameDominanceResult = cache.getOrCompute("early-game-dominance:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayerAcc(
            var games: Int = 0,
            var firstBloodGames: Int = 0,
            var firstBloodWins: Int = 0,
            var noFirstBloodWins: Int = 0,
            var noFirstBloodGames: Int = 0,
            var firstTowerGames: Int = 0,
            var totalKills: Int = 0,
        )

        val accMap = mutableMapOf<String, PlayerAcc>()

        var overallFirstBloodWins = 0
        var overallFirstBloodTotal = 0
        var overallFirstTowerWins = 0
        var overallFirstTowerTotal = 0

        for (match in matches) {
            // Determine which teams had first blood and first tower
            val firstBloodTeam = match.participants.firstOrNull { it.firstBloodKill }?.teamId
            val firstTowerTeam = match.participants.firstOrNull { it.firstTowerKill }?.teamId

            // Overall stats
            if (firstBloodTeam != null) {
                overallFirstBloodTotal++
                val fbTeamWon = match.participants.any { it.teamId == firstBloodTeam && it.win }
                if (fbTeamWon) overallFirstBloodWins++
            }
            if (firstTowerTeam != null) {
                overallFirstTowerTotal++
                val ftTeamWon = match.participants.any { it.teamId == firstTowerTeam && it.win }
                if (ftTeamWon) overallFirstTowerWins++
            }

            for (p in match.participants) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                acc.games++
                acc.totalKills += p.kills

                val involvedInFirstBlood = p.firstBloodKill || p.firstBloodAssist
                if (involvedInFirstBlood) {
                    acc.firstBloodGames++
                    if (p.win) acc.firstBloodWins++
                } else {
                    acc.noFirstBloodGames++
                    if (p.win) acc.noFirstBloodWins++
                }

                val involvedInFirstTower = p.firstTowerKill || p.firstTowerAssist
                if (involvedInFirstTower) acc.firstTowerGames++
            }
        }

        val avgKillsMap = accMap.mapValues { (_, acc) ->
            if (acc.games > 0) acc.totalKills.toDouble() / acc.games else 0.0
        }
        val maxAvgKills = avgKillsMap.values.maxOrNull() ?: 1.0

        val rawEntries = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                val firstBloodRate = acc.firstBloodGames.toDouble() / g
                val firstTowerRate = acc.firstTowerGames.toDouble() / g
                val avgKills = acc.totalKills.toDouble() / g
                val killsNorm = avgKills / maxOf(1.0, maxAvgKills)
                val earlyGameScore = firstBloodRate * 0.5 + firstTowerRate * 0.3 + killsNorm * 0.2

                val firstBloodWinRate = if (acc.firstBloodGames > 0)
                    (acc.firstBloodWins * 100.0 / acc.firstBloodGames).toInt() else 0
                val noFirstBloodWinRate = if (acc.noFirstBloodGames > 0)
                    (acc.noFirstBloodWins * 100.0 / acc.noFirstBloodGames).toInt() else 0

                riotId to EarlyGameDominanceEntry(
                    riotId = riotId,
                    games = acc.games,
                    firstBloodRate = r2(firstBloodRate),
                    firstTowerRate = r2(firstTowerRate),
                    earlyGameScore = r2(earlyGameScore),
                    firstBloodWinRate = firstBloodWinRate,
                    noFirstBloodWinRate = noFirstBloodWinRate,
                    badges = emptyList(),
                )
            }

        val firstBloodKing = rawEntries.maxByOrNull { it.second.firstBloodRate }?.first
        val towerDestroyer = rawEntries.maxByOrNull { it.second.firstTowerRate }?.first
        val earlyScoreKing = rawEntries.maxByOrNull { it.second.earlyGameScore }?.first

        val rankings = rawEntries.map { (riotId, entry) ->
            val badges = mutableListOf<String>()
            if (riotId == firstBloodKing) badges.add("퍼블킹")
            if (riotId == towerDestroyer) badges.add("포탑파괴자")
            if (riotId == earlyScoreKing) badges.add("초반지배자")
            entry.copy(badges = badges)
        }.sortedByDescending { it.earlyGameScore }

        val overallFirstBloodWinRate = if (overallFirstBloodTotal > 0)
            r2(overallFirstBloodWins.toDouble() / overallFirstBloodTotal) else 0.0
        val overallFirstTowerWinRate = if (overallFirstTowerTotal > 0)
            r2(overallFirstTowerWins.toDouble() / overallFirstTowerTotal) else 0.0

        EarlyGameDominanceResult(
            rankings = rankings,
            firstBloodKing = firstBloodKing,
            towerDestroyer = towerDestroyer,
            overallFirstBloodWinRate = overallFirstBloodWinRate,
            overallFirstTowerWinRate = overallFirstTowerWinRate,
        )
    }
}
