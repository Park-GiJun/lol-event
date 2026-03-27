package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.PlayerComparisonResult
import com.gijun.main.application.dto.stats.result.PlayerStatSnapshot
import com.gijun.main.application.port.`in`.GetPlayerComparisonUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.model.match.MatchParticipant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPlayerComparisonHandler(private val matchPersistencePort: MatchPersistencePort) : GetPlayerComparisonUseCase {

    override fun getPlayerComparison(player1: String, player2: String, mode: String): PlayerComparisonResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        fun r1(v: Double) = (v * 10).toInt() / 10.0
        fun r2(v: Double) = (v * 100).toInt() / 100.0

        val sharedMatches = matches.filter { m ->
            m.participants.any { it.riotId == player1 } && m.participants.any { it.riotId == player2 }
        }

        data class SnapAcc(
            val riotId: String,
            var games: Int = 0,
            var wins: Int = 0,
            var kills: Int = 0,
            var deaths: Int = 0,
            var assists: Int = 0,
            var damage: Long = 0L,
            var cs: Int = 0,
            var gold: Long = 0L,
            var visionScore: Int = 0,
        )

        fun SnapAcc.toSnapshot(): PlayerStatSnapshot {
            val g = games.coerceAtLeast(1)
            val totalKA = kills + assists
            return PlayerStatSnapshot(
                riotId = riotId,
                games = games,
                wins = wins,
                winRate = wins * 100 / g,
                avgKills = r1(kills.toDouble() / g),
                avgDeaths = r1(deaths.toDouble() / g),
                avgAssists = r1(assists.toDouble() / g),
                kda = if (deaths > 0) r2(totalKA.toDouble() / deaths) else totalKA.toDouble(),
                avgDamage = r1(damage.toDouble() / g),
                avgCs = r1(cs.toDouble() / g),
                avgGold = r1(gold.toDouble() / g),
                avgVisionScore = r1(visionScore.toDouble() / g),
            )
        }

        fun accumulate(acc: SnapAcc, p: MatchParticipant) {
            acc.games++
            if (p.win) acc.wins++
            acc.kills += p.kills
            acc.deaths += p.deaths
            acc.assists += p.assists
            acc.damage += p.damage
            acc.cs += p.cs
            acc.gold += p.gold
            acc.visionScore += p.visionScore
        }

        // Together (same team) accumulators
        val p1Together = SnapAcc(player1)
        val p2Together = SnapAcc(player2)
        var togetherGames = 0
        var togetherWins = 0

        // Versus (opposite team) accumulators
        val p1Versus = SnapAcc(player1)
        val p2Versus = SnapAcc(player2)
        var versusGames = 0
        var p1VersusWins = 0

        for (m in sharedMatches) {
            val p1p = m.participants.find { it.riotId == player1 } ?: continue
            val p2p = m.participants.find { it.riotId == player2 } ?: continue

            if (p1p.teamId == p2p.teamId) {
                // same team
                togetherGames++
                if (p1p.win) togetherWins++
                accumulate(p1Together, p1p)
                accumulate(p2Together, p2p)
            } else {
                // opposite teams
                versusGames++
                if (p1p.win) p1VersusWins++
                accumulate(p1Versus, p1p)
                accumulate(p2Versus, p2p)
            }
        }

        // Overall stats across all matches (not just shared)
        val p1Overall = SnapAcc(player1)
        val p2Overall = SnapAcc(player2)
        for (m in matches) {
            m.participants.find { it.riotId == player1 }?.let { accumulate(p1Overall, it) }
            m.participants.find { it.riotId == player2 }?.let { accumulate(p2Overall, it) }
        }

        return PlayerComparisonResult(
            player1 = player1,
            player2 = player2,
            togetherGames = togetherGames,
            togetherWinRate = if (togetherGames > 0) togetherWins * 100 / togetherGames else 0,
            p1TogetherStats = if (p1Together.games > 0) p1Together.toSnapshot() else null,
            p2TogetherStats = if (p2Together.games > 0) p2Together.toSnapshot() else null,
            versusGames = versusGames,
            player1VsWinRate = if (versusGames > 0) p1VersusWins * 100 / versusGames else 0,
            p1VersusStats = if (p1Versus.games > 0) p1Versus.toSnapshot() else null,
            p2VersusStats = if (p2Versus.games > 0) p2Versus.toSnapshot() else null,
            overallP1Stats = p1Overall.toSnapshot(),
            overallP2Stats = p2Overall.toSnapshot(),
        )
    }
}
