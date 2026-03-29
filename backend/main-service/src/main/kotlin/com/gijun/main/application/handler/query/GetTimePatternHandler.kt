package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.DayPatternEntry
import com.gijun.main.application.dto.stats.result.HourPatternEntry
import com.gijun.main.application.dto.stats.result.TimePatternResult
import com.gijun.main.application.port.`in`.GetTimePatternUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetTimePatternHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
) : GetTimePatternUseCase {

    override fun getTimePattern(mode: String): TimePatternResult = cache.getOrCompute("time-pattern:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))
        if (matches.isEmpty()) return@getOrCompute TimePatternResult(emptyList(), emptyList(), null, null, 0)

        val zone = ZoneId.of("Asia/Seoul")
        val DAY_NAMES = mapOf(1 to "월", 2 to "화", 3 to "수", 4 to "목", 5 to "금", 6 to "토", 7 to "일")

        data class DayAcc(var games: Int = 0, var wins: Int = 0, val sessionDates: MutableSet<String> = mutableSetOf())
        data class HourAcc(var games: Int = 0, var wins: Int = 0)

        val dayMap = mutableMapOf<Int, DayAcc>()    // 1-7 (Mon-Sun ISO)
        val hourMap = mutableMapOf<Int, HourAcc>()  // 0-23

        for (match in matches) {
            val dt = Instant.ofEpochMilli(match.gameCreation).atZone(zone)
            val dow = dt.dayOfWeek.value  // 1=Mon..7=Sun
            val hour = dt.hour
            val dateStr = "${dt.year}-${dt.monthValue}-${dt.dayOfMonth}"

            val teamWins = match.participants.filter { it.win }.map { it.team }.toSet()
            val matchWinCount = if (teamWins.isNotEmpty()) 1 else 0

            val dayAcc = dayMap.getOrPut(dow) { DayAcc() }
            dayAcc.games++
            dayAcc.wins += matchWinCount
            dayAcc.sessionDates.add(dateStr)

            val hourAcc = hourMap.getOrPut(hour) { HourAcc() }
            hourAcc.games++
        }

        val byDay = (1..7).mapNotNull { dow ->
            val acc = dayMap[dow] ?: return@mapNotNull null
            DayPatternEntry(
                dayOfWeek = dow,
                dayName = DAY_NAMES[dow] ?: "$dow",
                sessions = acc.sessionDates.size,
                games = acc.games,
                winRate = if (acc.games > 0) (acc.wins.toDouble() / acc.games * 100 * 10).toInt() / 10.0 else 0.0,
            )
        }

        val byHour = (0..23).mapNotNull { h ->
            val acc = hourMap[h] ?: return@mapNotNull null
            HourPatternEntry(hour = h, games = acc.games, winRate = 0.0)
        }

        val busiestDay = byDay.maxByOrNull { it.games }?.dayName
        val busiestHour = byHour.maxByOrNull { it.games }?.hour

        TimePatternResult(
            byDay = byDay,
            byHour = byHour,
            busiestDay = busiestDay,
            busiestHour = busiestHour,
            totalGames = matches.size,
        )
    }
}
