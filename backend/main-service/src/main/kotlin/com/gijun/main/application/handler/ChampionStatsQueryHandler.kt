package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetChampionCertificateQuery
import com.gijun.main.application.dto.query.GetChampionMatchupQuery
import com.gijun.main.application.dto.query.GetChampionStatsQuery
import com.gijun.main.application.dto.query.GetChampionTierQuery
import com.gijun.main.application.dto.query.GetLaneChampionsQuery
import com.gijun.main.application.dto.result.BanAnalysisResult
import com.gijun.main.application.dto.result.BanEntry
import com.gijun.main.application.dto.result.ChampionCertEntry
import com.gijun.main.application.dto.result.ChampionCertificateResult
import com.gijun.main.application.dto.result.ChampionDetailStats
import com.gijun.main.application.dto.result.ChampionItemStat
import com.gijun.main.application.dto.result.ChampionLaneStat
import com.gijun.main.application.dto.result.ChampionLaneStrength
import com.gijun.main.application.dto.result.ChampionMatchupResult
import com.gijun.main.application.dto.result.ChampionPlayerStat
import com.gijun.main.application.dto.result.ChampionRuneStat
import com.gijun.main.application.dto.result.ChampionTierEntry
import com.gijun.main.application.dto.result.ChampionTierResult
import com.gijun.main.application.dto.result.LaneGap
import com.gijun.main.application.dto.result.MatchupStat
import com.gijun.main.application.port.`in`.GetBanAnalysisUseCase
import com.gijun.main.application.port.`in`.GetChampionCertificateUseCase
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.`in`.GetChampionTierUseCase
import com.gijun.main.application.port.`in`.GetLaneChampionsUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.StatsCacheQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.model.MatchParticipantModel
import com.gijun.main.domain.match.service.PositionResolver
import com.gijun.main.domain.stats.service.RankingScore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 챔피언 축 통계 — 챔피언 상세, 맞대결, 장인 인증, 티어, 밴 분석.
 */
@Service
@Transactional(readOnly = true)
class ChampionStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsCacheQueryPersistencePort: StatsCacheQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetChampionStatsUseCase,
    GetChampionMatchupUseCase,
    GetLaneChampionsUseCase,
    GetChampionCertificateUseCase,
    GetChampionTierUseCase,
    GetBanAnalysisUseCase {
    override fun getChampionStats(query: GetChampionStatsQuery): ChampionDetailStats =
        statsResultCacheQueryPort.getOrCompute("champion-stats:${query.champion}:${query.mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            // query.champion 이름과 일치하는 참가자 수집 (대소문자 무시)
            val pairs =
                matches.flatMap { m ->
                    m.participants
                        .filter { it.champion.equals(query.champion, ignoreCase = true) }
                        .map { it to m }
                }

            if (pairs.isEmpty()) {
                return@getOrCompute ChampionDetailStats(
                    champion = query.champion,
                    championId = 0,
                    totalGames = 0,
                    totalWins = 0,
                    winRate = 0,
                    players = emptyList(),
                )
            }

            val championName = pairs.first().first.champion
            val championId = pairs.first().first.championId
            val totalGames = pairs.size
            val totalWins = pairs.count { (p, _) -> p.win }

            fun r1(v: Double) = (v * 10).toInt() / 10.0

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            fun kda(
                k: Int,
                d: Int,
                a: Int,
            ) = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()

            val players =
                pairs
                    .groupBy { (p, _) -> p.riotId }
                    .map { (riotId, entries) ->
                        val gs = entries.size
                        val ws = entries.count { (p, _) -> p.win }
                        val k = entries.sumOf { (p, _) -> p.kills }
                        val d = entries.sumOf { (p, _) -> p.deaths }
                        val a = entries.sumOf { (p, _) -> p.assists }
                        ChampionPlayerStat(
                            riotId = riotId,
                            games = gs,
                            wins = ws,
                            winRate = ws * 100 / gs,
                            avgKills = r1(k.toDouble() / gs),
                            avgDeaths = r1(d.toDouble() / gs),
                            avgAssists = r1(a.toDouble() / gs),
                            kda = kda(k, d, a),
                            avgDamage = entries.sumOf { (p, _) -> p.damage } / gs,
                            avgCs = r1(entries.sumOf { (p, _) -> p.cs }.toDouble() / gs),
                            avgGold = entries.sumOf { (p, _) -> p.gold } / gs,
                            avgVisionScore = r1(entries.sumOf { (p, _) -> p.visionScore }.toDouble() / gs),
                        )
                    }.sortedByDescending { it.games }

            // ── 아이템 통계 — 배치 캐시 우선, 없으면 실시간 계산 ────────
            val cachedItems = statsCacheQueryPersistencePort.findChampionItemCacheByChampionAndMode(championName, query.mode)
            val itemStats: List<ChampionItemStat> =
                if (cachedItems.isNotEmpty()) {
                    cachedItems.map { c ->
                        ChampionItemStat(itemId = c.itemId, picks = c.picks, wins = c.wins, winRate = c.winRate)
                    }
                } else {
                    val trinketIds = setOf(3340, 3363, 3364, 2052, 2055)
                    pairs
                        .flatMap { (p, _) ->
                            listOf(p.item0, p.item1, p.item2, p.item3, p.item4, p.item5)
                                .filter { it > 0 && it !in trinketIds }
                                .map { it to p.win }
                        }.groupBy { it.first }
                        .map { (itemId, entries) ->
                            val iWins = entries.count { it.second }
                            ChampionItemStat(
                                itemId = itemId,
                                picks = entries.size,
                                wins = iWins,
                                winRate = if (entries.isNotEmpty()) iWins * 100 / entries.size else 0,
                            )
                        }.sortedByDescending { it.picks }
                        .take(6)
                }

            // ── 룬 통계 — 배치 캐시 우선, 없으면 실시간 계산 ────────────
            // perk0(핵심 룬)이 0 인 참가자는 룬 정보가 안 실려 온 경기다. 세면 "룬 없음"이 1위가 된다.
            val cachedRunes = statsCacheQueryPersistencePort.findChampionRuneCacheByChampionAndMode(championName, query.mode)
            val runeStats: List<ChampionRuneStat> =
                if (cachedRunes.isNotEmpty()) {
                    cachedRunes.map { c ->
                        ChampionRuneStat(
                            keystone = c.keystone,
                            primaryStyle = c.primaryStyle,
                            subStyle = c.subStyle,
                            picks = c.picks,
                            wins = c.wins,
                            winRate = c.winRate,
                        )
                    }
                } else {
                    pairs
                        .filter { (p, _) -> p.perk0 > 0 && p.perkPrimaryStyle > 0 }
                        .groupBy { (p, _) -> Triple(p.perk0, p.perkPrimaryStyle, p.perkSubStyle) }
                        .map { (key, entries) ->
                            val rWins = entries.count { (p, _) -> p.win }
                            ChampionRuneStat(
                                keystone = key.first,
                                primaryStyle = key.second,
                                subStyle = key.third,
                                picks = entries.size,
                                wins = rWins,
                                winRate = rWins * 100 / entries.size,
                            )
                        }.sortedByDescending { it.picks }
                        .take(5)
                }

            // ── 라인별 통계 ──────────────────────────────────────────────
            val positionOrder = listOf("TOP", "JUNGLE", "MID", "BOTTOM", "SUPPORT")
            val laneStats =
                pairs
                    .mapNotNull { (p, _) ->
                        val pos = PositionResolver.resolve(p) ?: return@mapNotNull null
                        pos to p
                    }.groupBy { it.first }
                    .map { (pos, entries) ->
                        val ps = entries.map { it.second }
                        val lg = ps.size
                        val lw = ps.count { it.win }
                        val lk = ps.sumOf { it.kills }
                        val ld = ps.sumOf { it.deaths }
                        val la = ps.sumOf { it.assists }
                        ChampionLaneStat(
                            position = pos,
                            games = lg,
                            wins = lw,
                            winRate = lw * 100 / lg,
                            avgKills = r1(lk.toDouble() / lg),
                            avgDeaths = r1(ld.toDouble() / lg),
                            avgAssists = r1(la.toDouble() / lg),
                            kda = kda(lk, ld, la),
                            avgDamage = ps.sumOf { it.damage } / lg,
                            avgCs = r1(ps.sumOf { it.cs }.toDouble() / lg),
                            avgGold = ps.sumOf { it.gold } / lg,
                        )
                    }.sortedBy { positionOrder.indexOf(it.position).let { i -> if (i == -1) 99 else i } }

            ChampionDetailStats(
                champion = championName,
                championId = championId,
                totalGames = totalGames,
                totalWins = totalWins,
                winRate = totalWins * 100 / totalGames,
                players = players,
                itemStats = itemStats,
                runeStats = runeStats,
                laneStats = laneStats,
            )
        }

    /** 라인전 1:1. 같은 라인에 정확히 한 명씩일 때만 만든다. */
    private data class Duel(
        val me: MatchParticipantModel,
        val opp: MatchParticipantModel,
        val position: String,
    )

    /**
     * 챔피언 상성.
     *
     * 예전 구현은 두 가지가 잘못돼 있었다.
     *
     * 1. samePosition 이 기본 false 라 탑 챔피언과 상대 서포터가 "상성" 으로 잡혔다.
     *    한 경기에서 한 사람이 상대 다섯 명과 전부 짝지어지니 표본은 다섯 배로 부풀지만
     *    내용은 라인전과 아무 관계가 없다. 이제 같은 라인끼리만 맞춘다.
     * 2. 관측 승률로 정렬해서 1경기 1승이 맨 위에 왔다. 축소 보정 값으로 정렬한다.
     *
     * 그리고 승률만으로는 "왜 유리한지" 를 못 본다. 상대 라이너 대비 골드·CS·딜량·킬·시야
     * 격차를 같이 낸다. 이게 상성의 실제 내용이다.
     */
    override fun getChampionMatchup(query: GetChampionMatchupQuery): ChampionMatchupResult =
        statsResultCacheQueryPort.getOrCompute("champion-matchup:${query.champion}:${query.vsChampion}:${query.mode.key}") {
            val duels = buildDuels(query.mode)

            when {
                query.champion != null -> {
                    val mine = duels.filter { it.me.champion.equals(query.champion, ignoreCase = true) }
                    val name = mine.firstOrNull()?.me?.champion ?: query.champion
                    val id = mine.firstOrNull()?.me?.championId ?: 0
                    ChampionMatchupResult(
                        champion = name,
                        championId = id,
                        laneStrength = laneStrength(mine),
                        matchups = matchups(mine) { it.opp },
                        minGames = RankingScore.MIN_GAMES,
                    )
                }

                query.vsChampion != null -> {
                    // 카운터 관점 — 이 챔피언을 상대한 쪽의 성적. 시점을 뒤집어 같은 계산을 쓴다.
                    val against =
                        duels
                            .filter { it.opp.champion.equals(query.vsChampion, ignoreCase = true) }
                            .map { Duel(me = it.opp, opp = it.me, position = it.position) }
                    val name = against.firstOrNull()?.me?.champion ?: query.vsChampion
                    val id = against.firstOrNull()?.me?.championId ?: 0
                    ChampionMatchupResult(
                        champion = name,
                        championId = id,
                        laneStrength = laneStrength(against),
                        matchups = matchups(against) { it.opp },
                        minGames = RankingScore.MIN_GAMES,
                    )
                }

                else -> ChampionMatchupResult("", 0, emptyList(), emptyList(), RankingScore.MIN_GAMES)
            }
        }

    override fun getLaneChampions(query: GetLaneChampionsQuery): List<ChampionLaneStrength> =
        statsResultCacheQueryPort.getOrCompute("lane-champions:${query.position}:${query.mode.key}") {
            buildDuels(query.mode)
                .filter { it.position == query.position }
                .groupBy { it.me.champion }
                .values
                .flatMap { laneStrength(it) }
                // 관측 승률로 줄 세우면 1판 1승이 맨 위에 온다. 보정 값으로 세운다.
                .sortedWith(compareByDescending<ChampionLaneStrength> { it.adjustedWinRate }.thenByDescending { it.games })
        }

    // ────────── 라인전 1:1 만들기 ──────────

    private fun buildDuels(mode: GameMode): List<Duel> {
        val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)
        return matches.flatMap { m ->
            val teams = m.participants.groupBy { it.teamId }
            if (teams.size != 2) return@flatMap emptyList()
            val (a, b) = teams.values.toList()

            // 같은 라인에 정확히 한 명씩일 때만 맞춘다. 포지션이 깨진 팀은 조용히 건너뛴다.
            val byPosB = b.groupBy { PositionResolver.resolve(it) }
            a
                .mapNotNull { me ->
                    val pos = PositionResolver.resolve(me) ?: return@mapNotNull null
                    val opp = byPosB[pos]?.singleOrNull() ?: return@mapNotNull null
                    Duel(me, opp, pos)
                }.flatMap { d ->
                    // 양쪽 관점을 모두 담는다. a 팀만 담으면 b 팀 챔피언 조회가 비게 된다.
                    listOf(d, Duel(me = d.opp, opp = d.me, position = d.position))
                }
        }
    }

    // ────────── 집계 ──────────

    private fun gapOf(duels: List<Duel>): LaneGap {
        val n = duels.size.coerceAtLeast(1)

        fun avg(pick: (Duel) -> Double) = duels.sumOf(pick) / n
        return LaneGap(
            goldDiff = avg { (it.me.gold - it.opp.gold).toDouble() }.toInt(),
            csDiff = r1(avg { (it.me.cs - it.opp.cs).toDouble() }),
            damageDiff = avg { (it.me.damage - it.opp.damage).toDouble() }.toInt(),
            killDiff = r1(avg { (it.me.kills - it.opp.kills).toDouble() }),
            visionDiff = r1(avg { (it.me.visionScore - it.opp.visionScore).toDouble() }),
        )
    }

    /** 챔피언 x 라인. 개별 상성보다 표본이 두터워 이쪽이 실제로 읽힌다. */
    private fun laneStrength(duels: List<Duel>): List<ChampionLaneStrength> =
        duels
            .groupBy { it.position }
            .map { (pos, group) ->
                val w = group.count { it.me.win }
                ChampionLaneStrength(
                    champion = group.first().me.champion,
                    championId = group.first().me.championId,
                    position = pos,
                    games = group.size,
                    wins = w,
                    winRate = w * 100 / group.size,
                    adjustedWinRate = r2(RankingScore.shrunkWinRate(w, group.size)),
                    sampleGrade = RankingScore.sampleGrade(group.size),
                    gap = gapOf(group),
                )
            }.sortedByDescending { it.games }

    private fun matchups(
        duels: List<Duel>,
        key: (Duel) -> MatchParticipantModel,
    ): List<MatchupStat> =
        duels
            .groupBy { key(it).champion }
            .map { (_, group) ->
                val w = group.count { it.me.win }
                MatchupStat(
                    opponent = key(group.first()).champion,
                    opponentId = key(group.first()).championId,
                    position = group.first().position,
                    games = group.size,
                    wins = w,
                    winRate = w * 100 / group.size,
                    adjustedWinRate = r2(RankingScore.shrunkWinRate(w, group.size)),
                    sampleGrade = RankingScore.sampleGrade(group.size),
                    gap = gapOf(group),
                )
            }.filter { it.games >= RankingScore.MIN_GAMES }
            .sortedWith(compareByDescending<MatchupStat> { it.adjustedWinRate }.thenByDescending { it.games })

    private fun r1(v: Double) = Math.round(v * 10) / 10.0

    private fun r2(v: Double) = Math.round(v * 100) / 100.0

    private fun r1Certificate(v: Double) = (v * 10).toInt() / 10.0

    private fun r2Certificate(v: Double) = (v * 100).toInt() / 100.0

    override fun getChampionCertificate(query: GetChampionCertificateQuery): ChampionCertificateResult =
        statsResultCacheQueryPort.getOrCompute("champion-cert:${query.mode.key}:${query.minGames}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            data class PlayRecord(
                val riotId: String,
                val champion: String,
                val championId: Int,
                val win: Boolean,
                val kills: Int,
                val deaths: Int,
                val assists: Int,
                val damage: Int,
            )

            // Group by (riotId, champion)
            val grouped =
                matches
                    .flatMap { m ->
                        m.participants.map { p ->
                            PlayRecord(p.riotId, p.champion, p.championId, p.win, p.kills, p.deaths, p.assists, p.damage)
                        }
                    }.groupBy { "${it.riotId}||${it.champion}" }

            val allEntries =
                grouped.map { (_, records) ->
                    val g = records.size
                    val w = records.count { it.win }
                    val winRate = w * 100 / g
                    val k = records.sumOf { it.kills }
                    val d = records.sumOf { it.deaths }
                    val a = records.sumOf { it.assists }
                    val kda = if (d > 0) r2Certificate((k + a).toDouble() / d) else (k + a).toDouble()
                    val avgDamage = r2Certificate(records.sumOf { it.damage }.toDouble() / g)
                    val rep = records.first()
                    ChampionCertEntry(
                        riotId = rep.riotId,
                        champion = rep.champion,
                        championId = rep.championId,
                        games = g,
                        wins = w,
                        winRate = winRate,
                        avgKills = r1Certificate(k.toDouble() / g),
                        avgDeaths = r1Certificate(d.toDouble() / g),
                        avgAssists = r1Certificate(a.toDouble() / g),
                        kda = kda,
                        avgDamage = avgDamage,
                        adjustedWinRate = r2Certificate(RankingScore.shrunkWinRate(w, g)),
                        sampleGrade = RankingScore.sampleGrade(g),
                        // 관측 승률로 인증하면 1경기 1승이 곧바로 "장인"이 된다.
                        // 표본을 50% 쪽으로 당긴 값이 50을 넘어야 인증한다 — 표본이 쌓여야만 통과한다.
                        certified = g >= query.minGames && RankingScore.shrunkWinRate(w, g) >= 50.0,
                    )
                }

            // 관측 승률로 정렬하면 1경기 1승(100%)이 33경기 20승(60%)을 이긴다.
            val certifiedMasters =
                allEntries
                    .filter { it.certified }
                    .sortedWith(compareByDescending<ChampionCertEntry> { it.adjustedWinRate }.thenByDescending { it.games })

            // topChampionMasters: for each champion, the player with best win rate (among those with >= query.minGames)
            val topChampionMasters =
                allEntries
                    .filter { it.games >= query.minGames }
                    .groupBy { it.champion }
                    .mapValues { (_, entries) -> entries.maxBy { it.adjustedWinRate } }

            ChampionCertificateResult(
                certifiedMasters = certifiedMasters,
                topChampionMasters = topChampionMasters,
            )
        }

    override fun getChampionTier(query: GetChampionTierQuery): ChampionTierResult =
        statsResultCacheQueryPort.getOrCompute("champion-tier:${query.mode.key}:${query.minGames}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            val totalMatches = matches.size
            if (totalMatches == 0) return@getOrCompute ChampionTierResult(emptyList(), emptyMap(), 0)

            data class ChampionAcc(
                val champion: String,
                var championId: Int = 0,
                var games: Int = 0,
                var wins: Int = 0,
                var kills: Int = 0,
                var deaths: Int = 0,
                var assists: Int = 0,
                var totalDamage: Long = 0L,
            )

            val champMap = mutableMapOf<String, ChampionAcc>()

            for (m in matches) {
                for (p in m.participants) {
                    val acc = champMap.getOrPut(p.champion) { ChampionAcc(p.champion) }
                    acc.championId = p.championId
                    acc.games++
                    if (p.win) acc.wins++
                    acc.kills += p.kills
                    acc.deaths += p.deaths
                    acc.assists += p.assists
                    acc.totalDamage += p.damage
                }
            }

            // 전체 평균 KDA, 전체 평균 데미지
            val allParticipants = matches.flatMap { it.participants }
            val overallAvgKda =
                run {
                    val k = allParticipants.sumOf { it.kills }
                    val d = allParticipants.sumOf { it.deaths }
                    val a = allParticipants.sumOf { it.assists }
                    if (d > 0) (k + a).toDouble() / d else (k + a).toDouble()
                }
            val overallAvgDamage =
                if (allParticipants.isNotEmpty()) {
                    allParticipants.sumOf { it.damage }.toDouble() / allParticipants.size
                } else {
                    1.0
                }

            // 티어 점수 계산 (query.minGames 미만 포함 모두 계산, 티어만 "?"로)
            val scored =
                champMap.values.map { acc ->
                    val g = acc.games.coerceAtLeast(1)
                    val avgKda =
                        if (acc.deaths > 0) {
                            (acc.kills + acc.assists).toDouble() / acc.deaths
                        } else {
                            (acc.kills + acc.assists).toDouble()
                        }
                    val avgDamage = acc.totalDamage.toDouble() / g
                    val pickRate = acc.games.toDouble() / totalMatches

                    // 승률·KDA·데미지를 각각 전체 평균 쪽으로 끌어당긴 뒤 합성한다.
                    // 셋 중 하나라도 원값을 쓰면 1경기 챔피언이 그 항목 하나로 상위권에 올라온다.
                    val winRate = RankingScore.shrinkToward(acc.wins.toDouble() / g, 0.5, acc.games)
                    val normalizedKda = RankingScore.shrinkToward(avgKda / maxOf(1.0, overallAvgKda), 1.0, acc.games)
                    val dmgShare = RankingScore.shrinkToward(avgDamage / maxOf(1.0, overallAvgDamage), 1.0, acc.games)
                    // 픽률은 경기 수 그 자체라 표본 노이즈가 없다. 보정하지 않는다.
                    val tierScore = winRate * 0.5 + normalizedKda * 0.25 + dmgShare * 0.15 + pickRate * 0.10

                    acc to tierScore
                }

            // 백분위 티어 분류 (query.minGames 이상인 챔피언만 대상)
            val qualifiedScores =
                scored
                    .filter { (acc, _) -> acc.games >= query.minGames }
                    .map { (_, score) -> score }
                    .sorted()

            fun tierForScore(
                score: Double,
                qualified: Boolean,
            ): String {
                if (!qualified) return "?"
                if (qualifiedScores.isEmpty()) return "B"
                val rank = qualifiedScores.indexOfFirst { it >= score }.let { if (it == -1) qualifiedScores.size else it }
                val percentile = rank.toDouble() / qualifiedScores.size // 0 = 최하위, 1 = 최상위
                val topPct = 1.0 - percentile
                return when {
                    topPct <= 0.15 -> "S"
                    topPct <= 0.35 -> "A"
                    topPct <= 0.60 -> "B"
                    topPct <= 0.80 -> "C"
                    else -> "D"
                }
            }

            val tierList =
                scored
                    .map { (acc, tierScore) ->
                        val g = acc.games.coerceAtLeast(1)
                        val avgDamage = acc.totalDamage.toDouble() / g
                        val avgKda =
                            if (acc.deaths > 0) {
                                r2((acc.kills + acc.assists).toDouble() / acc.deaths)
                            } else {
                                (acc.kills + acc.assists).toDouble()
                            }
                        val pickRate = acc.games.toDouble() / totalMatches
                        val qualified = acc.games >= query.minGames
                        ChampionTierEntry(
                            champion = acc.champion,
                            championId = acc.championId,
                            tier = tierForScore(tierScore, qualified),
                            tierScore = r2(tierScore),
                            games = acc.games,
                            winRate = acc.wins * 100 / g,
                            adjustedWinRate = r2(RankingScore.shrunkWinRate(acc.wins, acc.games)),
                            sampleGrade = RankingScore.sampleGrade(acc.games),
                            kda = avgKda,
                            pickRate = r2(pickRate),
                            avgDamage = r2(avgDamage),
                        )
                    }
                    // 표본 미달 챔피언은 목록에 남기되 항상 뒤로 보낸다.
                    // 점수만으로 정렬하면 "?" 티어가 상단을 차지해 티어표가 거짓말을 한다.
                    .sortedWith(
                        compareByDescending<ChampionTierEntry> { it.games >= query.minGames }
                            .thenByDescending { it.tierScore },
                    )

            val byTier = tierList.groupBy { it.tier }

            ChampionTierResult(
                tierList = tierList,
                byTier = byTier,
                totalMatches = totalMatches,
            )
        }

    override fun getBanAnalysis(mode: GameMode): BanAnalysisResult =
        statsResultCacheQueryPort.getOrCompute("ban-analysis:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)
            if (matches.isEmpty()) return@getOrCompute BanAnalysisResult(emptyList(), 0, null)

            val totalGames = matches.size
            val banCountMap = mutableMapOf<Int, Pair<String, Int>>() // championId -> (name, count)

            for (match in matches) {
                for (team in match.teams) {
                    for (ban in team.bans) {
                        if (ban.championId <= 0) continue
                        val current = banCountMap[ban.championId]
                        banCountMap[ban.championId] = Pair(ban.championName, (current?.second ?: 0) + 1)
                    }
                }
            }

            val topBanned =
                banCountMap.entries
                    .map { (id, pair) ->
                        BanEntry(
                            champion = pair.first,
                            championId = id,
                            banCount = pair.second,
                            banRate = (pair.second.toDouble() / totalGames * 100).let { (it * 10).toInt() / 10.0 },
                        )
                    }.sortedByDescending { it.banCount }
                    .take(20)

            BanAnalysisResult(
                topBanned = topBanned,
                totalGamesAnalyzed = totalGames,
                mostBannedChampion = topBanned.firstOrNull()?.champion,
            )
        }
}
