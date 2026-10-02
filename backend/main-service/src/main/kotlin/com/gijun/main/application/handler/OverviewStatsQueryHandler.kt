package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.ChampionPickStat
import com.gijun.main.application.dto.result.OverviewStats
import com.gijun.main.application.dto.result.PlayerLeaderStat
import com.gijun.main.application.dto.result.WeeklyAwardEntry
import com.gijun.main.application.dto.result.WeeklyAwardsResult
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.`in`.GetWeeklyAwardsUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.model.MatchParticipantModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 전체 요약 — 개요 지표와 주간 어워드.
 */
@Service
@Transactional(readOnly = true)
class OverviewStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetOverviewStatsUseCase,
    GetWeeklyAwardsUseCase {
    private fun r1(v: Double) = (v * 10).toInt() / 10.0

    private fun r2(v: Double) = (v * 100).toInt() / 100.0

    /** 참가자 + 해당 경기의 게임 시간(초) */
    private data class Entry(
        val p: MatchParticipantModel,
        val durationSec: Int,
    ) {
        val minutes: Double get() = if (durationSec > 0) durationSec / 60.0 else 1.0
    }

    override fun getOverviewStats(mode: GameMode): OverviewStats =
        statsResultCacheQueryPort.getOrCompute("overview-stats:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)
            val allTeams = matches.flatMap { it.teams }

            // 참가자에 게임시간 결합
            val allEntries =
                matches.flatMap { m ->
                    m.participants.map { Entry(it, m.gameDuration) }
                }
            val allP = allEntries.map { it.p }

            // ── 챔피언 픽 통계 ────────────────────────────────────
            val champStats =
                allP.groupBy { it.champion }.map { (champ, ps) ->
                    val wins = ps.count { it.win }
                    val n = ps.size
                    val totalK = ps.sumOf { it.kills }
                    val totalD = ps.sumOf { it.deaths }
                    val totalA = ps.sumOf { it.assists }
                    ChampionPickStat(
                        champion = champ,
                        championId = ps.first().championId,
                        picks = n,
                        wins = wins,
                        winRate = wins * 100 / n,
                        kda = if (totalD > 0) r2((totalK + totalA).toDouble() / totalD) else (totalK + totalA).toDouble(),
                        avgKills = r1(totalK.toDouble() / n),
                        avgDeaths = r1(totalD.toDouble() / n),
                        avgAssists = r1(totalA.toDouble() / n),
                        avgDamage = ps.sumOf { it.damage } / n,
                        avgCs = r1(ps.sumOf { it.cs }.toDouble() / n),
                    )
                }
            val topPicked = champStats.sortedByDescending { it.picks }.take(20)
            val topWinRate = champStats.filter { it.picks >= 3 }.sortedByDescending { it.winRate }.take(8)

            // ── 챔피언 밴 통계 ────────────────────────────────────
            val topBanned =
                allTeams
                    .flatMap { it.bans }
                    .filter { it.championId > 0 }
                    .groupBy { it.championId }
                    .map { (champId, bans) ->
                        ChampionPickStat(
                            champion = bans.first().championName,
                            championId = champId,
                            picks = bans.size,
                            wins = 0,
                            winRate = 0,
                        )
                    }.sortedByDescending { it.picks }
                    .take(15)

            // ── 플레이어별 집계 ───────────────────────────────────
            val byPlayer = allEntries.groupBy { it.p.riotId }
            val defaultMinGames = 3

            /**
             * @param minGames 최소 경기 수
             * @param score    플레이어 점수 함수 (높을수록 우수)
             * @param display  표시 문자열 함수
             */
            fun leader(
                minGames: Int = defaultMinGames,
                score: (List<Entry>) -> Double,
                display: (List<Entry>, Double) -> String,
            ): PlayerLeaderStat? =
                byPlayer
                    .filter { it.value.size >= minGames }
                    .mapValues { (_, es) -> score(es) }
                    .maxByOrNull { it.value }
                    ?.let { (riotId, v) ->
                        val entries = byPlayer.getValue(riotId)
                        PlayerLeaderStat(riotId, display(entries, v), entries.size)
                    }

            // 공통 계산 헬퍼
            fun perMin(
                es: List<Entry>,
                sum: (Entry) -> Int,
            ): Double {
                val total = es.sumOf(sum).toDouble()
                val minutes = es.sumOf { it.durationSec } / 60.0
                return if (minutes > 0) total / minutes else 0.0
            }

            fun perGame(
                es: List<Entry>,
                sum: (Entry) -> Int,
            ): Double = es.sumOf(sum).toDouble() / es.size

            fun kdaOf(es: List<Entry>): Double {
                val k = es.sumOf { it.p.kills }
                val d = es.sumOf { it.p.deaths }
                val a = es.sumOf { it.p.assists }
                return if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()
            }

            // ── 명예의 전당 — per match ────────────────────────────
            val winRateLeader =
                leader(
                    score = { es -> perGame(es) { if (it.p.win) 1 else 0 } * 100 },
                    display = { _, v -> "${v.toInt()}%" },
                )

            val kdaLeader =
                leader(
                    score = { es -> kdaOf(es) },
                    display = { _, v -> "${r2(v)} KDA" },
                )

            val killsLeader =
                leader(
                    score = { es -> perGame(es) { it.p.kills } },
                    display = { _, v -> "${r1(v)} 킬/경기" },
                )

            // ── 명예의 전당 — per minute ──────────────────────────
            val damageLeader =
                leader(
                    score = { es -> perMin(es) { it.p.damage } },
                    display = { _, v -> "${v.toLong().toLocaleString()}/분" },
                )

            val goldLeader =
                leader(
                    score = { es -> perMin(es) { it.p.gold } },
                    display = { _, v -> "${v.toLong().toLocaleString()}/분" },
                )

            val csLeader =
                leader(
                    score = { es -> perMin(es) { it.p.cs } },
                    display = { _, v -> "${r1(v)} CS/분" },
                )

            val visionLeader =
                leader(
                    score = { es -> perMin(es) { it.p.visionScore } },
                    display = { _, v -> "${r1(v)} 점/분" },
                )

            val objectiveDamageLeader =
                leader(
                    score = { es -> perMin(es) { it.p.damageDealtToObjectives } },
                    display = { _, v -> "${v.toLong().toLocaleString()}/분" },
                )

            // ── 명예의 전당 — per match (기타) ───────────────────
            val turretKillsLeader =
                leader(
                    score = { es -> perGame(es) { it.p.turretKills } },
                    display = { _, v -> "${r1(v)} 포탑/경기" },
                )

            val pentaKillsLeader =
                leader(
                    minGames = 1,
                    score = { es -> es.sumOf { it.p.pentaKills }.toDouble() },
                    display = { _, v -> "${v.toInt()}회" },
                )

            val wardsLeader =
                leader(
                    score = { es -> perGame(es) { it.p.wardsPlaced } },
                    display = { _, v -> "${r1(v)} 개/경기" },
                )

            val ccLeader =
                leader(
                    score = { es -> perMin(es) { it.p.timeCCingOthers } },
                    display = { _, v -> "${r1(v)} 초/분" },
                )

            val mostGamesPlayed =
                byPlayer
                    .maxByOrNull { it.value.size }
                    ?.let { (riotId, es) -> PlayerLeaderStat(riotId, "${es.size}판", es.size) }

            val firstBloodLeader =
                leader(
                    minGames = 1,
                    score = { es -> es.count { it.p.firstBloodKill }.toDouble() },
                    display = { _, v -> "${v.toInt()}회" },
                )

            // ── 전체 오브젝트 + 경기 시간 집계 ──────────────────
            val totalDurationSec = matches.sumOf { it.gameDuration }
            val avgGameMin =
                if (matches.isNotEmpty()) {
                    r1(totalDurationSec.toDouble() / matches.size / 60.0)
                } else {
                    0.0
                }

            OverviewStats(
                matchCount = matches.size,
                avgGameMinutes = avgGameMin,
                topPickedChampions = topPicked,
                topWinRateChampions = topWinRate,
                topBannedChampions = topBanned,
                winRateLeader = winRateLeader,
                kdaLeader = kdaLeader,
                killsLeader = killsLeader,
                damageLeader = damageLeader,
                goldLeader = goldLeader,
                csLeader = csLeader,
                visionLeader = visionLeader,
                objectiveDamageLeader = objectiveDamageLeader,
                turretKillsLeader = turretKillsLeader,
                pentaKillsLeader = pentaKillsLeader,
                wardsLeader = wardsLeader,
                ccLeader = ccLeader,
                mostGamesPlayed = mostGamesPlayed,
                firstBloodLeader = firstBloodLeader,
                totalBaronKills = allTeams.sumOf { it.baronKills },
                totalDragonKills = allTeams.sumOf { it.dragonKills },
                totalTowerKills = allTeams.sumOf { it.towerKills },
                totalRiftHeraldKills = allTeams.sumOf { it.riftHeraldKills },
                totalInhibitorKills = allTeams.sumOf { it.inhibitorKills },
                totalFirstBloods = matches.count { m -> m.teams.any { it.firstBlood } },
                totalCs = allP.sumOf { it.cs }.toLong(),
            )
        }

    private fun Long.toLocaleString() = String.format("%,d", this)

    override fun getWeeklyAwards(mode: GameMode): WeeklyAwardsResult =
        statsResultCacheQueryPort.getOrCompute("weekly-awards:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            // 1. mostDeaths: 단일 경기 최다 사망
            data class SingleMatchDeaths(
                val riotId: String,
                val deaths: Int,
            )
            val mostDeathsEntry =
                matches
                    .flatMap { match ->
                        match.participants.map { p -> SingleMatchDeaths(p.riotId, p.deaths) }
                    }.maxByOrNull { it.deaths }
                    ?.let { entry ->
                        WeeklyAwardEntry(
                            riotId = entry.riotId,
                            displayValue = entry.deaths.toString(),
                            games = 1,
                        )
                    }

            // 플레이어별 집계 (게임 수, KDA 합계, 골드, 데미지, 승패, 항복 유발, 펜타킬 등)
            data class PlayerAcc(
                var games: Int = 0,
                var wins: Int = 0,
                var totalKills: Int = 0,
                var totalDeaths: Int = 0,
                var totalAssists: Int = 0,
                var totalGold: Int = 0,
                var totalDamage: Int = 0,
                var surrenderCount: Int = 0,
                var pentaKillTotal: Int = 0,
                var loneHeroCount: Int = 0,
                val championPlayCount: MutableMap<String, Int> = mutableMapOf(),
            )

            val accMap = mutableMapOf<String, PlayerAcc>()

            for (match in matches) {
                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                    acc.games++
                    if (p.win) acc.wins++
                    acc.totalKills += p.kills
                    acc.totalDeaths += p.deaths
                    acc.totalAssists += p.assists
                    acc.totalGold += p.gold
                    acc.totalDamage += p.damage
                    if (p.causedEarlySurrender) acc.surrenderCount++
                    acc.pentaKillTotal += p.pentaKills
                    // loneHero: 팀이 졌지만 개인 KDA >= 10
                    if (!p.win) {
                        val kda = (p.kills + p.assists).toDouble() / maxOf(p.deaths, 1)
                        if (kda >= 10.0) acc.loneHeroCount++
                    }
                    acc.championPlayCount[p.champion] = (acc.championPlayCount[p.champion] ?: 0) + 1
                }
            }

            // 2. worstKda: 최소 5게임, 평균 KDA 최하위
            val worstKdaEntry =
                accMap.entries
                    .filter { it.value.games >= 5 }
                    .minByOrNull { (_, acc) ->
                        (acc.totalKills + acc.totalAssists).toDouble() / maxOf(acc.totalDeaths, 1)
                    }?.let { (riotId, acc) ->
                        val kda = r2((acc.totalKills + acc.totalAssists).toDouble() / maxOf(acc.totalDeaths, 1))
                        WeeklyAwardEntry(riotId = riotId, displayValue = kda.toString(), games = acc.games)
                    }

            // 3. highGoldLowDamage: 골드 상위 30%인데 데미지 하위 30% ("먹튀")
            val allPlayers = accMap.entries.filter { it.value.games > 0 }
            val sortedByGold = allPlayers.sortedByDescending { it.value.totalGold }
            val sortedByDamage = allPlayers.sortedBy { it.value.totalDamage }
            val topGoldThreshold = maxOf(1, (allPlayers.size * 0.3).toInt())
            val lowDamageThreshold = maxOf(1, (allPlayers.size * 0.3).toInt())
            val topGoldRiotIds = sortedByGold.take(topGoldThreshold).map { it.key }.toSet()
            val lowDamageRiotIds = sortedByDamage.take(lowDamageThreshold).map { it.key }.toSet()
            val highGoldLowDamageCandidates = topGoldRiotIds.intersect(lowDamageRiotIds)
            val highGoldLowDamageEntry =
                highGoldLowDamageCandidates
                    .mapNotNull { riotId -> accMap[riotId]?.let { riotId to it } }
                    .minByOrNull { (_, acc) ->
                        acc.totalDamage.toDouble() / maxOf(acc.totalGold, 1)
                    }?.let { (riotId, acc) ->
                        val ratio = r2(acc.totalDamage.toDouble() / maxOf(acc.totalGold, 1))
                        WeeklyAwardEntry(riotId = riotId, displayValue = ratio.toString(), games = acc.games)
                    }

            // 4. mostSurrenders: causedEarlySurrender 총 횟수 최다
            val mostSurrendersEntry =
                accMap.entries
                    .filter { it.value.surrenderCount > 0 }
                    .maxByOrNull { it.value.surrenderCount }
                    ?.let { (riotId, acc) ->
                        WeeklyAwardEntry(riotId = riotId, displayValue = acc.surrenderCount.toString(), games = acc.games)
                    }

            // 5. pentaKillHero: 펜타킬 합산 최다
            val pentaKillHeroEntry =
                accMap.entries
                    .filter { it.value.pentaKillTotal > 0 }
                    .maxByOrNull { it.value.pentaKillTotal }
                    ?.let { (riotId, acc) ->
                        WeeklyAwardEntry(riotId = riotId, displayValue = acc.pentaKillTotal.toString(), games = acc.games)
                    }

            // 6. loneHero: 팀이 졌지만 개인 KDA 10 이상인 경기 횟수 최다
            val loneHeroEntry =
                accMap.entries
                    .filter { it.value.loneHeroCount > 0 }
                    .maxByOrNull { it.value.loneHeroCount }
                    ?.let { (riotId, acc) ->
                        WeeklyAwardEntry(riotId = riotId, displayValue = acc.loneHeroCount.toString(), games = acc.games)
                    }

            // 7. highestWinRate: 최소 5게임 중 최고 승률
            val highestWinRateEntry =
                accMap.entries
                    .filter { it.value.games >= 5 }
                    .maxByOrNull { it.value.wins.toDouble() / it.value.games }
                    ?.let { (riotId, acc) ->
                        val winRate = r2(acc.wins.toDouble() / acc.games * 100)
                        WeeklyAwardEntry(riotId = riotId, displayValue = "$winRate%", games = acc.games)
                    }

            // 8. mostGamesChampion: 단일 챔피언 최다 플레이 (챔피언명 포함)
            data class ChampionPlayEntry(
                val riotId: String,
                val champion: String,
                val count: Int,
                val totalGames: Int,
            )
            val mostGamesChampionEntry =
                accMap.entries
                    .flatMap { (riotId, acc) ->
                        acc.championPlayCount.entries.map { (champ, cnt) ->
                            ChampionPlayEntry(riotId, champ, cnt, acc.games)
                        }
                    }.maxByOrNull { it.count }
                    ?.let { entry ->
                        WeeklyAwardEntry(
                            riotId = entry.riotId,
                            displayValue = "${entry.champion}(${entry.count})",
                            games = entry.totalGames,
                        )
                    }

            WeeklyAwardsResult(
                mostDeaths = mostDeathsEntry,
                worstKda = worstKdaEntry,
                highGoldLowDamage = highGoldLowDamageEntry,
                mostSurrenders = mostSurrendersEntry,
                pentaKillHero = pentaKillHeroEntry,
                loneHero = loneHeroEntry,
                highestWinRate = highestWinRateEntry,
                mostGamesChampion = mostGamesChampionEntry,
            )
        }
}
