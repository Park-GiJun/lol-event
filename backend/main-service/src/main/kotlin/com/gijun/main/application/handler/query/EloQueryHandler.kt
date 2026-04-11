package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.EloHistoryEntry
import com.gijun.main.application.dto.stats.result.EloLeaderboardResult
import com.gijun.main.application.dto.stats.result.EloRankEntry
import com.gijun.main.application.dto.stats.result.PlayerEloHistoryResult
import com.gijun.main.application.port.`in`.GetEloHistoryUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetEloUseCase
import com.gijun.main.application.port.out.EloHistoryPort
import com.gijun.main.application.port.out.EloPort
import com.gijun.main.domain.model.elo.PlayerElo
import org.springframework.stereotype.Service

@Service
class EloQueryHandler(
    private val eloPort: EloPort,
    private val eloHistoryPort: EloHistoryPort,
) : GetEloUseCase, GetEloLeaderboardUseCase, GetEloHistoryUseCase {

    companion object {
        private const val INITIAL_ELO = 1000.0
    }

    override fun getAll(): List<PlayerElo> = eloPort.findAll()
    override fun getByRiotId(riotId: String): PlayerElo? = eloPort.findByRiotId(riotId)

    override fun getHistory(riotId: String, limit: Int): PlayerEloHistoryResult {
        val allElos = eloPort.findAll().sortedByDescending { it.elo }
        val myElo   = allElos.firstOrNull { it.riotId == riotId }
        val currentElo = myElo?.elo ?: INITIAL_ELO
        val eloRank    = allElos.indexOfFirst { it.riotId == riotId }.takeIf { it >= 0 }?.plus(1)
        val history    = eloHistoryPort.findByRiotId(riotId, limit).map {
            EloHistoryEntry(
                matchId = it.matchId, eloBefore = it.eloBefore, eloAfter = it.eloAfter,
                delta = it.delta, win = it.win, gameCreation = it.gameCreation,
            )
        }
        return PlayerEloHistoryResult(riotId = riotId, currentElo = currentElo, eloRank = eloRank, history = history)
    }

    override fun getLeaderboard(): EloLeaderboardResult {
        val sorted = eloPort.findAll().sortedByDescending { it.elo }
        return EloLeaderboardResult(
            players = sorted.mapIndexed { idx, elo ->
                val wr = if (elo.games > 0) elo.wins * 100.0 / elo.games else 0.0
                EloRankEntry(
                    rank = idx + 1, riotId = elo.riotId, elo = elo.elo,
                    games = elo.games, wins = elo.wins, losses = elo.losses,
                    winRate = (wr * 10).toLong() / 10.0,
                    winStreak = elo.winStreak, lossStreak = elo.lossStreak,
                )
            }
        )
    }
}
