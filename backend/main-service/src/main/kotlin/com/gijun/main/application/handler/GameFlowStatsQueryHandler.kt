package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.ChampionLengthTendency
import com.gijun.main.application.dto.result.ComebackIndexEntry
import com.gijun.main.application.dto.result.ComebackIndexResult
import com.gijun.main.application.dto.result.ComebackMatchEntry
import com.gijun.main.application.dto.result.DayPatternEntry
import com.gijun.main.application.dto.result.EarlyGameDominanceEntry
import com.gijun.main.application.dto.result.EarlyGameDominanceResult
import com.gijun.main.application.dto.result.GameLengthBucket
import com.gijun.main.application.dto.result.GameLengthTendencyEntry
import com.gijun.main.application.dto.result.GameLengthTendencyResult
import com.gijun.main.application.dto.result.HourPatternEntry
import com.gijun.main.application.dto.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.result.ObjectiveStat
import com.gijun.main.application.dto.result.TimePatternResult
import com.gijun.main.application.port.`in`.GetComebackIndexUseCase
import com.gijun.main.application.port.`in`.GetEarlyGameDominanceUseCase
import com.gijun.main.application.port.`in`.GetGameLengthTendencyUseCase
import com.gijun.main.application.port.`in`.GetObjectiveCorrelationUseCase
import com.gijun.main.application.port.`in`.GetTimePatternUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.match.model.MatchTeamModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId

/**
 * 경기 흐름 통계 — 시간대 패턴, 경기 길이 성향, 초반 지배, 역전 지수, 오브젝트 상관.
 */
@Service
@Transactional(readOnly = true)
class GameFlowStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetTimePatternUseCase,
    GetGameLengthTendencyUseCase,
    GetEarlyGameDominanceUseCase,
    GetComebackIndexUseCase,
    GetObjectiveCorrelationUseCase {
    override fun getTimePattern(mode: GameMode): TimePatternResult =
        statsResultCacheQueryPort.getOrCompute("time-pattern:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)
            if (matches.isEmpty()) return@getOrCompute TimePatternResult(emptyList(), emptyList(), null, null, 0)

            val zone = ZoneId.of("Asia/Seoul")
            val dayNames = mapOf(1 to "월", 2 to "화", 3 to "수", 4 to "목", 5 to "금", 6 to "토", 7 to "일")

            data class DayAcc(
                var games: Int = 0,
                var wins: Int = 0,
                val sessionDates: MutableSet<String> = mutableSetOf(),
            )

            data class HourAcc(
                var games: Int = 0,
                var wins: Int = 0,
            )

            val dayMap = mutableMapOf<Int, DayAcc>() // 1-7 (Mon-Sun ISO)
            val hourMap = mutableMapOf<Int, HourAcc>() // 0-23

            for (match in matches) {
                val dt = Instant.ofEpochMilli(match.gameCreation).atZone(zone)
                val dow = dt.dayOfWeek.value // 1=Mon..7=Sun
                val hour = dt.hour
                val dateStr = "${dt.year}-${dt.monthValue}-${dt.dayOfMonth}"

                val teamWins =
                    match.participants
                        .filter { it.win }
                        .map { it.team }
                        .toSet()
                val matchWinCount = if (teamWins.isNotEmpty()) 1 else 0

                val dayAcc = dayMap.getOrPut(dow) { DayAcc() }
                dayAcc.games++
                dayAcc.wins += matchWinCount
                dayAcc.sessionDates.add(dateStr)

                val hourAcc = hourMap.getOrPut(hour) { HourAcc() }
                hourAcc.games++
            }

            val byDay =
                (1..7).mapNotNull { dow ->
                    val acc = dayMap[dow] ?: return@mapNotNull null
                    DayPatternEntry(
                        dayOfWeek = dow,
                        dayName = dayNames[dow] ?: "$dow",
                        sessions = acc.sessionDates.size,
                        games = acc.games,
                    )
                }

            val byHour =
                (0..23).mapNotNull { h ->
                    val acc = hourMap[h] ?: return@mapNotNull null
                    HourPatternEntry(hour = h, games = acc.games)
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

    private enum class LengthBand { SHORT, MID, LONG }

    private fun bandOf(gameDuration: Int): LengthBand =
        when {
            gameDuration < 1500 -> LengthBand.SHORT
            gameDuration < 2100 -> LengthBand.MID
            else -> LengthBand.LONG
        }

    override fun getGameLengthTendency(mode: GameMode): GameLengthTendencyResult =
        statsResultCacheQueryPort.getOrCompute("game-length-tendency:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            fun r1(v: Double) = (v * 10).toInt() / 10.0

            data class BucketAcc(
                var games: Int = 0,
                var wins: Int = 0,
                var kills: Int = 0,
                var deaths: Int = 0,
                var damage: Long = 0L,
                var cs: Int = 0,
                var durationSec: Long = 0L,
            )

            fun BucketAcc.toBucket(): GameLengthBucket {
                val g = games.coerceAtLeast(1)
                val durationMin = durationSec.toDouble() / 60.0
                val avgMin = if (games > 0) durationMin / games else 1.0
                return GameLengthBucket(
                    games = games,
                    wins = wins,
                    winRate = if (games > 0) wins * 100 / games else 0,
                    avgKills = r1(kills.toDouble() / g),
                    avgDeaths = r1(deaths.toDouble() / g),
                    avgDamage = r1(damage.toDouble() / g),
                    avgCsPerMin = if (avgMin > 0) r1(cs.toDouble() / g / avgMin) else 0.0,
                )
            }

            // 플레이어별 구간별 집계
            data class PlayerAcc(
                val short: BucketAcc = BucketAcc(),
                val mid: BucketAcc = BucketAcc(),
                val long: BucketAcc = BucketAcc(),
            )

            val playerMap = mutableMapOf<String, PlayerAcc>()

            // 챔피언별 구간별 집계
            data class ChampAcc(
                val champion: String,
                var championId: Int = 0,
                val short: BucketAcc = BucketAcc(),
                val mid: BucketAcc = BucketAcc(),
                val long: BucketAcc = BucketAcc(),
            )

            val champMap = mutableMapOf<String, ChampAcc>()

            for (m in matches) {
                val band = bandOf(m.gameDuration)
                for (p in m.participants) {
                    val pAcc = playerMap.getOrPut(p.riotId) { PlayerAcc() }
                    val bAcc =
                        when (band) {
                            LengthBand.SHORT -> pAcc.short
                            LengthBand.MID -> pAcc.mid
                            LengthBand.LONG -> pAcc.long
                        }
                    bAcc.games++
                    if (p.win) bAcc.wins++
                    bAcc.kills += p.kills
                    bAcc.deaths += p.deaths
                    bAcc.damage += p.damage
                    bAcc.cs += p.cs
                    bAcc.durationSec += m.gameDuration

                    val cAcc = champMap.getOrPut(p.champion) { ChampAcc(p.champion) }
                    cAcc.championId = p.championId
                    val cbAcc =
                        when (band) {
                            LengthBand.SHORT -> cAcc.short
                            LengthBand.MID -> cAcc.mid
                            LengthBand.LONG -> cAcc.long
                        }
                    cbAcc.games++
                    if (p.win) cbAcc.wins++
                    cbAcc.durationSec += m.gameDuration
                }
            }

            val players =
                playerMap.entries
                    .map { (riotId, acc) ->
                        val shortBucket = acc.short.toBucket()
                        val midBucket = acc.mid.toBucket()
                        val longBucket = acc.long.toBucket()
                        val totalGames = acc.short.games + acc.mid.games + acc.long.games

                        val sWr = shortBucket.winRate
                        val lWr = longBucket.winRate
                        val tendency =
                            when {
                                sWr > lWr + 15 -> "단기전형"
                                lWr > sWr + 15 -> "장기전형"
                                sWr > 55 && lWr > 55 -> "전천후"
                                else -> "중기전형"
                            }

                        GameLengthTendencyEntry(
                            riotId = riotId,
                            totalGames = totalGames,
                            shortGame = shortBucket,
                            midGame = midBucket,
                            longGame = longBucket,
                            tendency = tendency,
                        )
                    }.sortedByDescending { it.totalGames }

            val championTendencies =
                champMap.values
                    .map { acc ->
                        val sWr = if (acc.short.games > 0) acc.short.wins * 100 / acc.short.games else 0
                        val mWr = if (acc.mid.games > 0) acc.mid.wins * 100 / acc.mid.games else 0
                        val lWr = if (acc.long.games > 0) acc.long.wins * 100 / acc.long.games else 0
                        val bestLength =
                            when (maxOf(sWr, mWr, lWr)) {
                                sWr -> "단기전"
                                lWr -> "장기전"
                                else -> "중기전"
                            }
                        ChampionLengthTendency(
                            champion = acc.champion,
                            championId = acc.championId,
                            shortWinRate = sWr,
                            midWinRate = mWr,
                            longWinRate = lWr,
                            bestLength = bestLength,
                        )
                    }.sortedByDescending { acc -> acc.shortWinRate + acc.midWinRate + acc.longWinRate }

            GameLengthTendencyResult(
                players = players,
                championTendencies = championTendencies,
            )
        }

    fun r1(v: Double) = (v * 10).toInt() / 10.0

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getEarlyGameDominance(mode: GameMode): EarlyGameDominanceResult =
        statsResultCacheQueryPort.getOrCompute("early-game-dominance:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

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

            val avgKillsMap =
                accMap.mapValues { (_, acc) ->
                    if (acc.games > 0) acc.totalKills.toDouble() / acc.games else 0.0
                }
            val maxAvgKills = avgKillsMap.values.maxOrNull() ?: 1.0

            val rawEntries =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        val g = acc.games.toDouble()
                        val firstBloodRate = acc.firstBloodGames.toDouble() / g
                        val firstTowerRate = acc.firstTowerGames.toDouble() / g
                        val avgKills = acc.totalKills.toDouble() / g
                        val killsNorm = avgKills / maxOf(1.0, maxAvgKills)
                        val earlyGameScore = firstBloodRate * 0.5 + firstTowerRate * 0.3 + killsNorm * 0.2

                        val firstBloodWinRate =
                            if (acc.firstBloodGames > 0) {
                                (acc.firstBloodWins * 100.0 / acc.firstBloodGames).toInt()
                            } else {
                                0
                            }
                        val noFirstBloodWinRate =
                            if (acc.noFirstBloodGames > 0) {
                                (acc.noFirstBloodWins * 100.0 / acc.noFirstBloodGames).toInt()
                            } else {
                                0
                            }

                        riotId to
                            EarlyGameDominanceEntry(
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

            val rankings =
                rawEntries
                    .map { (riotId, entry) ->
                        val badges = mutableListOf<String>()
                        if (riotId == firstBloodKing) badges.add("퍼블킹")
                        if (riotId == towerDestroyer) badges.add("포탑파괴자")
                        if (riotId == earlyScoreKing) badges.add("초반지배자")
                        entry.copy(badges = badges)
                    }.sortedByDescending { it.earlyGameScore }

            val overallFirstBloodWinRate =
                if (overallFirstBloodTotal > 0) {
                    r2(overallFirstBloodWins.toDouble() / overallFirstBloodTotal)
                } else {
                    0.0
                }
            val overallFirstTowerWinRate =
                if (overallFirstTowerTotal > 0) {
                    r2(overallFirstTowerWins.toDouble() / overallFirstTowerTotal)
                } else {
                    0.0
                }

            EarlyGameDominanceResult(
                rankings = rankings,
                firstBloodKing = firstBloodKing,
                towerDestroyer = towerDestroyer,
                overallFirstBloodWinRate = overallFirstBloodWinRate,
                overallFirstTowerWinRate = overallFirstTowerWinRate,
            )
        }

    override fun getComebackIndex(mode: GameMode): ComebackIndexResult =
        statsResultCacheQueryPort.getOrCompute("comeback-index:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

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
                        ),
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
            val topComebackMatches =
                contestMatchCandidates
                    .sortedByDescending { it.gameDurationMin }
                    .take(5)

            val entries =
                accMap.entries
                    .filter { it.value.totalGames > 0 }
                    .map { (riotId, acc) ->
                        val totalWinRate =
                            if (acc.totalGames > 0) {
                                (acc.totalWins * 100.0 / acc.totalGames).toInt()
                            } else {
                                0
                            }
                        val contestWinRate =
                            if (acc.contestGames > 0) {
                                (acc.contestWins * 100.0 / acc.contestGames).toInt()
                            } else {
                                0
                            }
                        val surrenderWinRate =
                            if (acc.surrenderGames > 0) {
                                (acc.surrenderWins * 100.0 / acc.surrenderGames).toInt()
                            } else {
                                0
                            }
                        val comebackBonus = contestWinRate - totalWinRate
                        val isKing = acc.contestGames >= 5 && comebackBonus >= 10

                        ComebackIndexEntry(
                            riotId = riotId,
                            totalGames = acc.totalGames,
                            totalWinRate = totalWinRate,
                            contestGames = acc.contestGames,
                            contestWinRate = contestWinRate,
                            comebackBonus = comebackBonus,
                            isKing = isKing,
                        )
                    }.sortedByDescending { it.comebackBonus }

            val comebackKing =
                entries
                    .filter { it.isKing }
                    .maxByOrNull { it.comebackBonus }
                    ?.riotId

            ComebackIndexResult(
                rankings = entries,
                comebackKing = comebackKing,
                topComebackMatches = topComebackMatches,
            )
        }

    /**
     * 오브젝트 하나를 어느 팀이 챙겼는지 가리는 방법.
     *
     * 퍼블·첫 드래곤 같은 건 "먼저" 잡은 팀이 플래그로 기록된다.
     * 공허 유충은 그런 플래그가 없어서(horde_kills 카운트만 있다) 더 많이 먹은 쪽을 주인으로 본다.
     */
    private sealed interface Basis {
        /** 플래그가 켜진 팀. */
        data class First(
            val flag: (MatchTeamModel) -> Boolean,
        ) : Basis

        /** 더 많이 챙긴 팀. 같으면 주인을 못 가린다. */
        data class Majority(
            val count: (MatchTeamModel) -> Int,
        ) : Basis
    }

    private data class ObjConfig(
        val key: String,
        val label: String,
        val basis: Basis,
    )

    private val objectives =
        listOf(
            ObjConfig("firstBlood", "퍼스트 블러드", Basis.First { it.firstBlood }),
            ObjConfig("firstDragon", "첫 드래곤", Basis.First { it.firstDragon }),
            ObjConfig("firstBaron", "첫 바론", Basis.First { it.firstBaron }),
            ObjConfig("firstTower", "첫 포탑", Basis.First { it.firstTower }),
            ObjConfig("firstInhibitor", "첫 억제기", Basis.First { it.firstInhibitor }),
            // 공허 유충은 먼저 잡은 팀 표시가 없어 더 많이 먹은 쪽으로 본다.
            // 수집분 346팀 중 181팀에 값이 있다.
            ObjConfig("hordeKills", "공허 유충", Basis.Majority { it.hordeKills }),
        )

    /** 이 경기에서 해당 오브젝트를 챙긴 팀. 못 가리면 null. */
    private fun ownerOf(
        match: MatchModel,
        basis: Basis,
    ): MatchTeamModel? =
        when (basis) {
            is Basis.First -> match.teams.firstOrNull { basis.flag(it) }
            is Basis.Majority -> {
                val ranked = match.teams.sortedByDescending { basis.count(it) }
                val top = ranked.firstOrNull()
                val runnerUp = ranked.getOrNull(1)
                // 아무도 안 먹었거나 양 팀이 같으면 주인이 없다.
                when {
                    top == null || basis.count(top) == 0 -> null
                    runnerUp != null && basis.count(runnerUp) == basis.count(top) -> null
                    else -> top
                }
            }
        }

    private fun basisName(basis: Basis) =
        when (basis) {
            is Basis.First -> "FIRST"
            is Basis.Majority -> "MAJORITY"
        }

    override fun getObjectiveCorrelation(mode: GameMode): ObjectiveCorrelationResult =
        statsResultCacheQueryPort.getOrCompute("objective-correlation:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)
            val totalGames = matches.size

            if (totalGames == 0) return@getOrCompute ObjectiveCorrelationResult(0, emptyList())

            val stats =
                objectives.mapNotNull { cfg ->
                    var gamesWithOwner = 0
                    var winsWithOwner = 0

                    for (m in matches) {
                        val owner = ownerOf(m, cfg.basis) ?: continue
                        gamesWithOwner++
                        if (owner.win) winsWithOwner++
                    }

                    // 이 오브젝트가 한 번도 기록되지 않았으면 화면에 줄을 만들지 않는다.
                    // 0% 짜리 빈 칸이 하나 더 생기는 것뿐이라서다.
                    if (gamesWithOwner == 0) return@mapNotNull null

                    // 주인을 가린 경기에서 "챙긴 팀이 진" 비율이 곧 반대쪽 승률이다.
                    // 예전에는 주인이 없는 경기를 세면서 승자 유무만 보고 있어서, 주인이 가려진
                    // 경기의 반대편이 통째로 빠졌다.
                    val lossesWithOwner = gamesWithOwner - winsWithOwner

                    ObjectiveStat(
                        objective = cfg.key,
                        label = cfg.label,
                        basis = basisName(cfg.basis),
                        totalGames = totalGames,
                        gamesWithFirst = gamesWithOwner,
                        winsWithFirst = winsWithOwner,
                        winRateWithFirst = winsWithOwner * 100 / gamesWithOwner,
                        winRateWithout = lossesWithOwner * 100 / gamesWithOwner,
                    )
                }

            ObjectiveCorrelationResult(totalGames, stats)
        }
}
