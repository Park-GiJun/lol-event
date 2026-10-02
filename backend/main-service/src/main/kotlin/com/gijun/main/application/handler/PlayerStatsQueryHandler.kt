package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetGrowthCurveQuery
import com.gijun.main.application.dto.query.GetPlayerComparisonQuery
import com.gijun.main.application.dto.query.GetPlayerStatsQuery
import com.gijun.main.application.dto.query.GetPlayerStreakQuery
import com.gijun.main.application.dto.result.ChampionCount
import com.gijun.main.application.dto.result.ChampionStat
import com.gijun.main.application.dto.result.GrowthCurveEntry
import com.gijun.main.application.dto.result.GrowthCurveResult
import com.gijun.main.application.dto.result.LaneStat
import com.gijun.main.application.dto.result.PlayerComparisonResult
import com.gijun.main.application.dto.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.result.PlayerStatSnapshot
import com.gijun.main.application.dto.result.PlayerStatsResult
import com.gijun.main.application.dto.result.RecentMatchStat
import com.gijun.main.application.dto.result.StatsResult
import com.gijun.main.application.dto.result.StreakResult
import com.gijun.main.application.port.`in`.GetGrowthCurveUseCase
import com.gijun.main.application.port.`in`.GetPlayerComparisonUseCase
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.`in`.GetPlayerStreakUseCase
import com.gijun.main.application.port.`in`.GetStatsUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.application.port.out.persistence.PlayerRatingQueryPersistencePort
import com.gijun.main.application.port.out.persistence.StatsCacheQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.model.MatchParticipantModel
import com.gijun.main.domain.match.service.PositionResolver
import com.gijun.main.domain.rating.service.RatingMath
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 플레이어 축 통계 — 랭킹 표, 개인 상세, 연승·연패, 성장 곡선, 두 사람 비교.
 *
 * 전부 같은 경기 집합을 읽어 메모리에서 집계하고, 결과는 모드·파라미터를 키로 5 분간 캐시한다.
 */
@Service
@Transactional(readOnly = true)
class PlayerStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val memberQueryPersistencePort: MemberQueryPersistencePort,
    private val statsCacheQueryPersistencePort: StatsCacheQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
    private val playerRatingQueryPersistencePort: PlayerRatingQueryPersistencePort,
) : GetStatsUseCase,
    GetPlayerStatsUseCase,
    GetPlayerStreakUseCase,
    GetGrowthCurveUseCase,
    GetPlayerComparisonUseCase {
    override fun getStats(mode: GameMode): StatsResult =
        statsResultCacheQueryPort.getOrCompute("stats:${mode.key}") {
            val queueIds = mode.queueIds
            val matchCount = matchQueryPersistencePort.countByQueueIds(queueIds)

            // 배치가 집계한 스냅샷이 있으면 캐시 우선 사용 (중복 집계 없음)
            val cached = statsCacheQueryPersistencePort.findPlayerCacheByMode(mode)
            if (cached.isNotEmpty()) {
                val stats =
                    cached
                        .sortedWith(
                            compareByDescending<com.gijun.main.application.port.out.persistence.PlayerStatsCache> { it.winRate }
                                .thenByDescending { it.games },
                        ).map { c ->
                            PlayerStatsResult(
                                riotId = c.riotId,
                                games = c.games,
                                wins = c.wins,
                                losses = c.losses,
                                winRate = c.winRate,
                                avgKills = c.avgKills,
                                avgDeaths = c.avgDeaths,
                                avgAssists = c.avgAssists,
                                kda = c.kda,
                                avgDamage = c.avgDamage,
                                avgCs = c.avgCs,
                                avgGold = c.avgGold,
                                avgVisionScore = c.avgVisionScore,
                                topChampions = listOfNotNull(c.topChampion?.let { ChampionCount(it, 0) }),
                            )
                        }
                return@getOrCompute StatsResult(stats, matchCount)
            }

            // 캐시 없음 → 원본 계산 (배치 미실행 초기 상태)
            val members = memberQueryPersistencePort.findAll()
            val matches = matchQueryPersistencePort.findAllWithParticipants(queueIds)

            data class Acc(
                val riotId: String,
                var wins: Int = 0,
                var losses: Int = 0,
                var kills: Int = 0,
                var deaths: Int = 0,
                var assists: Int = 0,
                var damage: Int = 0,
                var cs: Int = 0,
                var gold: Int = 0,
                var visionScore: Int = 0,
                var games: Int = 0,
                val champions: MutableMap<String, Int> = mutableMapOf(),
            )

            val accByPuuid = members.associate { it.puuid to Acc(it.riotId) }.toMutableMap()
            val accByRiotId = members.associate { it.riotId to accByPuuid.getValue(it.puuid) }

            for (match in matches) {
                for (p in match.participants) {
                    val s = (p.puuid?.let { accByPuuid[it] }) ?: accByRiotId[p.riotId] ?: continue
                    s.games++
                    if (p.win) s.wins++ else s.losses++
                    s.kills += p.kills
                    s.deaths += p.deaths
                    s.assists += p.assists
                    s.damage += p.damage
                    s.cs += p.cs
                    s.gold += p.gold
                    s.visionScore += p.visionScore
                    s.champions[p.champion] = (s.champions[p.champion] ?: 0) + 1
                }
            }

            val stats =
                accByPuuid.values
                    .filter { it.games > 0 }
                    .map { s ->
                        PlayerStatsResult(
                            riotId = s.riotId,
                            games = s.games,
                            wins = s.wins,
                            losses = s.losses,
                            winRate = (s.wins * 100 / s.games),
                            avgKills = (s.kills.toDouble() / s.games * 10).toInt() / 10.0,
                            avgDeaths = (s.deaths.toDouble() / s.games * 10).toInt() / 10.0,
                            avgAssists = (s.assists.toDouble() / s.games * 10).toInt() / 10.0,
                            kda =
                                if (s.deaths > 0) {
                                    ((s.kills + s.assists).toDouble() / s.deaths * 100).toInt() / 100.0
                                } else {
                                    (s.kills + s.assists).toDouble()
                                },
                            avgDamage = s.damage / s.games,
                            avgCs = (s.cs.toDouble() / s.games * 10).toInt() / 10.0,
                            avgGold = s.gold / s.games,
                            avgVisionScore = (s.visionScore.toDouble() / s.games * 10).toInt() / 10.0,
                            topChampions =
                                s.champions.entries
                                    .sortedByDescending { it.value }
                                    .take(3)
                                    .map { ChampionCount(it.key, it.value) },
                        )
                    }.sortedWith(compareByDescending<PlayerStatsResult> { it.winRate }.thenByDescending { it.games })

            StatsResult(stats, matchCount)
        }

    override fun getPlayerStats(query: GetPlayerStatsQuery): PlayerDetailStatsResult =
        statsResultCacheQueryPort.getOrCompute("player-stats:${query.riotId.value}:${query.mode.key}:${query.lane}") {
            // 화면이 "Elo" 라고 부르는 값은 이제 실력 레이팅(laneElo)이다. 순위도 그 표시값 기준이다.
            // 이 DTO 의 elo 는 **표시값**이다 — 리더보드와 같은 숫자가 보여야 하므로 수축을 먹여 내려준다.
            // 원값이 필요하면 /api/admin/elo 를 쓴다.
            val allRatings = playerRatingQueryPersistencePort.findAll().sortedByDescending { it.laneEloDisplay }
            val playerRating = allRatings.firstOrNull { it.riotId == query.riotId.value }
            val eloRank = allRatings.indexOfFirst { it.riotId == query.riotId.value }.takeIf { it >= 0 }?.plus(1)

            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            data class Entry(
                val matchId: String,
                val queueId: Int,
                val gameCreation: Long,
                val gameDuration: Int,
                val champion: String,
                val championId: Int,
                val win: Boolean,
                val kills: Int,
                val deaths: Int,
                val assists: Int,
                val damage: Int,
                val cs: Int,
                val gold: Int,
                val visionScore: Int,
                val position: String?,
                val damageTaken: Int,
                val objectiveDamage: Int,
                val wardsPlaced: Int,
                val ccTime: Int,
                val neutralMinions: Int,
            )

            val entries =
                matches.flatMap { m ->
                    m.participants
                        .filter { it.riotId == query.riotId.value }
                        .map { p ->
                            Entry(
                                matchId = m.matchId,
                                queueId = m.queueId,
                                gameCreation = m.gameCreation,
                                gameDuration = m.gameDuration,
                                champion = p.champion,
                                championId = p.championId,
                                win = p.win,
                                kills = p.kills,
                                deaths = p.deaths,
                                assists = p.assists,
                                damage = p.damage,
                                cs = p.cs,
                                gold = p.gold,
                                visionScore = p.visionScore,
                                position = PositionResolver.resolve(p),
                                damageTaken = p.totalDamageTaken,
                                objectiveDamage = p.damageDealtToObjectives,
                                wardsPlaced = p.wardsPlaced,
                                ccTime = p.timeCCingOthers,
                                neutralMinions = p.neutralMinionsKilled,
                            )
                        }
                }

            val filteredEntries = if (query.lane != null) entries.filter { it.position == query.lane } else entries

            if (filteredEntries.isEmpty()) {
                return@getOrCompute PlayerDetailStatsResult(
                    riotId = query.riotId.value,
                    games = 0,
                    wins = 0,
                    losses = 0,
                    winRate = 0,
                    avgKills = 0.0,
                    avgDeaths = 0.0,
                    avgAssists = 0.0,
                    kda = 0.0,
                    avgDamage = 0,
                    avgCs = 0.0,
                    avgGold = 0,
                    avgVisionScore = 0.0,
                    elo = playerRating?.laneEloDisplay ?: RatingMath.START,
                    eloRank = eloRank,
                    championStats = emptyList(),
                    recentMatches = emptyList(),
                    laneStats = emptyList(),
                )
            }

            val games = filteredEntries.size
            val wins = filteredEntries.count { it.win }
            val kills = filteredEntries.sumOf { it.kills }
            val deaths = filteredEntries.sumOf { it.deaths }
            val assists = filteredEntries.sumOf { it.assists }

            fun r1(v: Double) = (v * 10).toInt() / 10.0

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            fun kda(
                k: Int,
                d: Int,
                a: Int,
            ) = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()

            val championStats =
                filteredEntries
                    .groupBy { it.champion }
                    .map { (champ, es) ->
                        val cg = es.size
                        val cw = es.count { it.win }
                        val ck = es.sumOf { it.kills }
                        val cd = es.sumOf { it.deaths }
                        val ca = es.sumOf { it.assists }
                        ChampionStat(
                            champion = champ,
                            championId = es.first().championId,
                            games = cg,
                            wins = cw,
                            winRate = cw * 100 / cg,
                            avgKills = r1(ck.toDouble() / cg),
                            avgDeaths = r1(cd.toDouble() / cg),
                            avgAssists = r1(ca.toDouble() / cg),
                            kda = kda(ck, cd, ca),
                            avgDamage = es.sumOf { it.damage } / cg,
                            avgCs = r1(es.sumOf { it.cs }.toDouble() / cg),
                            avgGold = es.sumOf { it.gold } / cg,
                        )
                    }.sortedByDescending { it.games }

            val recentMatches =
                filteredEntries.sortedByDescending { it.gameCreation }.take(20).map {
                    RecentMatchStat(
                        matchId = it.matchId,
                        champion = it.champion,
                        championId = it.championId,
                        win = it.win,
                        kills = it.kills,
                        deaths = it.deaths,
                        assists = it.assists,
                        damage = it.damage,
                        cs = it.cs,
                        gold = it.gold,
                        gameCreation = it.gameCreation,
                        gameDuration = it.gameDuration,
                        queueId = it.queueId,
                    )
                }

            // ── 포지션별 통계 (query.lane 필터 없이 전체 entries 기준) ──────────────────────────────────────
            val positionOrder = listOf("TOP", "JUNGLE", "MID", "BOTTOM", "SUPPORT")
            val laneStats =
                entries
                    .mapNotNull { entry -> entry.position?.let { it to entry } }
                    .groupBy({ it.first }, { it.second })
                    .map { (pos, es) ->
                        val pg = es.size
                        val pw = es.count { it.win }
                        val pk = es.sumOf { it.kills }
                        val pd = es.sumOf { it.deaths }
                        val pa = es.sumOf { it.assists }
                        LaneStat(
                            position = pos,
                            games = pg,
                            wins = pw,
                            winRate = pw * 100 / pg,
                            avgKills = r1(pk.toDouble() / pg),
                            avgDeaths = r1(pd.toDouble() / pg),
                            avgAssists = r1(pa.toDouble() / pg),
                            kda = kda(pk, pd, pa),
                            avgDamage = es.sumOf { it.damage } / pg,
                            avgCs = r1(es.sumOf { it.cs }.toDouble() / pg),
                            avgGold = es.sumOf { it.gold } / pg,
                            avgVisionScore = r1(es.sumOf { it.visionScore }.toDouble() / pg),
                            avgDamageTaken = es.sumOf { it.damageTaken } / pg,
                            avgObjectiveDamage = es.sumOf { it.objectiveDamage } / pg,
                            avgWardsPlaced = r1(es.sumOf { it.wardsPlaced }.toDouble() / pg),
                            avgCcTime = r1(es.sumOf { it.ccTime }.toDouble() / pg),
                            avgNeutralMinions = r1(es.sumOf { it.neutralMinions }.toDouble() / pg),
                        )
                    }.sortedBy { positionOrder.indexOf(it.position).let { i -> if (i == -1) 99 else i } }

            PlayerDetailStatsResult(
                riotId = query.riotId.value,
                games = games,
                wins = wins,
                losses = games - wins,
                winRate = wins * 100 / games,
                avgKills = r1(kills.toDouble() / games),
                avgDeaths = r1(deaths.toDouble() / games),
                avgAssists = r1(assists.toDouble() / games),
                kda = kda(kills, deaths, assists),
                avgDamage = filteredEntries.sumOf { it.damage } / games,
                avgCs = r1(filteredEntries.sumOf { it.cs }.toDouble() / games),
                avgGold = filteredEntries.sumOf { it.gold } / games,
                avgVisionScore = r1(filteredEntries.sumOf { it.visionScore }.toDouble() / games),
                elo = playerRating?.laneEloDisplay ?: RatingMath.START,
                eloRank = eloRank,
                championStats = championStats,
                recentMatches = recentMatches,
                laneStats = laneStats,
            )
        }

    override fun getPlayerStreak(query: GetPlayerStreakQuery): StreakResult =
        statsResultCacheQueryPort.getOrCompute("player-streak:${query.riotId.value}:${query.mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            // 해당 플레이어의 경기 결과를 최신순으로 수집
            val results =
                matches
                    .flatMap { m -> m.participants.filter { it.riotId == query.riotId.value }.map { it.win to m.gameCreation } }
                    .sortedByDescending { it.second }

            if (results.isEmpty()) return@getOrCompute StreakResult(query.riotId.value, 0, "NONE", 0, 0, emptyList(), 0, 0, 0)

            val wins = results.count { it.first }
            val losses = results.size - wins

            // 현재 연승/연패
            val isWinStreak = results.first().first
            var currentStreak = 0
            for ((win, _) in results) {
                if (win == isWinStreak) currentStreak++ else break
            }

            // 역대 최장 연승/연패
            var longestWin = 0
            var longestLoss = 0
            var curWin = 0
            var curLoss = 0
            for ((win, _) in results) {
                if (win) {
                    curWin++
                    curLoss = 0
                    if (curWin > longestWin) longestWin = curWin
                } else {
                    curLoss++
                    curWin = 0
                    if (curLoss > longestLoss) longestLoss = curLoss
                }
            }

            val recentForm = results.take(10).map { if (it.first) "W" else "L" }

            StreakResult(
                riotId = query.riotId.value,
                currentStreak = if (isWinStreak) currentStreak else -currentStreak,
                currentStreakType = if (isWinStreak) "WIN" else "LOSS",
                longestWinStreak = longestWin,
                longestLossStreak = longestLoss,
                recentForm = recentForm,
                totalGames = results.size,
                wins = wins,
                losses = losses,
            )
        }

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    private fun rollingAvg(
        list: List<Double>,
        index: Int,
        window: Int = 5,
    ): Double {
        val from = maxOf(0, index - window + 1)
        val slice = list.subList(from, index + 1)
        return if (slice.isEmpty()) 0.0 else slice.average()
    }

    override fun getGrowthCurve(query: GetGrowthCurveQuery): GrowthCurveResult =
        statsResultCacheQueryPort.getOrCompute("growth-curve:${query.riotId.value}:${query.mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            data class RawEntry(
                val matchId: String,
                val gameCreation: Long,
                val champion: String,
                val win: Boolean,
                val kda: Double,
                val dmgShare: Double,
                val visionPerMin: Double,
                val csPerMin: Double,
            )

            val rawEntries =
                matches
                    .mapNotNull { match ->
                        val p = match.participants.find { it.riotId == query.riotId.value } ?: return@mapNotNull null
                        val durationMin = maxOf(1.0, match.gameDuration / 60.0)
                        val teamTotalDamage =
                            match.participants
                                .filter { it.teamId == p.teamId }
                                .sumOf { it.damage }

                        val kda = (p.kills + p.assists).toDouble() / maxOf(1, p.deaths)
                        val dmgShare = p.damage.toDouble() / maxOf(1, teamTotalDamage)
                        val visionPerMin = p.visionScore / durationMin
                        val csPerMin = p.cs / durationMin

                        RawEntry(
                            matchId = match.matchId,
                            gameCreation = match.gameCreation,
                            champion = p.champion,
                            win = p.win,
                            kda = kda,
                            dmgShare = dmgShare,
                            visionPerMin = visionPerMin,
                            csPerMin = csPerMin,
                        )
                    }.sortedBy { it.gameCreation }

            val kdaList = rawEntries.map { it.kda }
            val dmgShareList = rawEntries.map { it.dmgShare }
            val csPerMinList = rawEntries.map { it.csPerMin }

            val entries =
                rawEntries.mapIndexed { idx, raw ->
                    GrowthCurveEntry(
                        matchId = raw.matchId,
                        gameCreation = raw.gameCreation,
                        champion = raw.champion,
                        win = raw.win,
                        kda = r2(raw.kda),
                        dmgShare = r2(raw.dmgShare),
                        visionPerMin = r2(raw.visionPerMin),
                        csPerMin = r2(raw.csPerMin),
                        rollingKda = r2(rollingAvg(kdaList, idx)),
                        rollingDmgShare = r2(rollingAvg(dmgShareList, idx)),
                        rollingCsPerMin = r2(rollingAvg(csPerMinList, idx)),
                    )
                }

            val overallAvgKda = if (kdaList.isEmpty()) 0.0 else r2(kdaList.average())
            val recentKdaList = kdaList.takeLast(5)
            val recentAvgKda = if (recentKdaList.isEmpty()) 0.0 else r2(recentKdaList.average())

            val trend =
                when {
                    overallAvgKda <= 0.0 -> "STABLE"
                    recentAvgKda >= overallAvgKda * 1.10 -> "IMPROVING"
                    recentAvgKda <= overallAvgKda * 0.90 -> "DECLINING"
                    else -> "STABLE"
                }

            GrowthCurveResult(
                riotId = query.riotId.value,
                entries = entries,
                totalGames = entries.size,
                recentAvgKda = recentAvgKda,
                overallAvgKda = overallAvgKda,
                trend = trend,
            )
        }

    override fun getPlayerComparison(query: GetPlayerComparisonQuery): PlayerComparisonResult =
        statsResultCacheQueryPort.getOrCompute("player-comparison:${query.player1.value}:${query.player2.value}:${query.mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            fun r1(v: Double) = (v * 10).toInt() / 10.0

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            val sharedMatches =
                matches.filter { m ->
                    m.participants.any { it.riotId == query.player1.value } && m.participants.any { it.riotId == query.player2.value }
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

            fun accumulate(
                acc: SnapAcc,
                p: MatchParticipantModel,
            ) {
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
            val p1Together = SnapAcc(query.player1.value)
            val p2Together = SnapAcc(query.player2.value)
            var togetherGames = 0
            var togetherWins = 0

            // Versus (opposite team) accumulators
            val p1Versus = SnapAcc(query.player1.value)
            val p2Versus = SnapAcc(query.player2.value)
            var versusGames = 0
            var p1VersusWins = 0

            for (m in sharedMatches) {
                val p1p = m.participants.find { it.riotId == query.player1.value } ?: continue
                val p2p = m.participants.find { it.riotId == query.player2.value } ?: continue

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
            val p1Overall = SnapAcc(query.player1.value)
            val p2Overall = SnapAcc(query.player2.value)
            for (m in matches) {
                m.participants.find { it.riotId == query.player1.value }?.let { accumulate(p1Overall, it) }
                m.participants.find { it.riotId == query.player2.value }?.let { accumulate(p2Overall, it) }
            }

            PlayerComparisonResult(
                player1 = query.player1.value,
                player2 = query.player2.value,
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
