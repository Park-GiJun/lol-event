package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.StreakResult
import com.gijun.main.application.port.`in`.GetPlayerStreakUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetPlayerStreakHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetPlayerStreakUseCase {

    override fun getPlayerStreak(riotId: String, mode: String): StreakResult = cache.getOrCompute("player-streak:$riotId:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        // 해당 플레이어의 경기 결과를 최신순으로 수집
        val results = matches
            .flatMap { m -> m.participants.filter { it.riotId == riotId }.map { it.win to m.gameCreation } }
            .sortedByDescending { it.second }

        if (results.isEmpty()) return@getOrCompute StreakResult(riotId, 0, "NONE", 0, 0, emptyList(), 0, 0, 0)

        val wins   = results.count { it.first }
        val losses = results.size - wins

        // 현재 연승/연패
        val isWinStreak = results.first().first
        var currentStreak = 0
        for ((win, _) in results) {
            if (win == isWinStreak) currentStreak++ else break
        }

        // 역대 최장 연승/연패
        var longestWin = 0; var longestLoss = 0
        var curWin = 0; var curLoss = 0
        for ((win, _) in results) {
            if (win) {
                curWin++; curLoss = 0
                if (curWin > longestWin) longestWin = curWin
            } else {
                curLoss++; curWin = 0
                if (curLoss > longestLoss) longestLoss = curLoss
            }
        }

        val recentForm = results.take(10).map { if (it.first) "W" else "L" }

        StreakResult(
            riotId            = riotId,
            currentStreak     = if (isWinStreak) currentStreak else -currentStreak,
            currentStreakType  = if (isWinStreak) "WIN" else "LOSS",
            longestWinStreak  = longestWin,
            longestLossStreak = longestLoss,
            recentForm        = recentForm,
            totalGames        = results.size,
            wins              = wins,
            losses            = losses,
        )
    }
}
