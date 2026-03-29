package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionStat
import com.gijun.main.application.dto.stats.result.LaneStat
import com.gijun.main.application.dto.stats.result.PlayerDetailStatsResult
import com.gijun.main.application.dto.stats.result.RecentMatchStat
import com.gijun.main.application.port.`in`.GetPlayerStatsUseCase
import com.gijun.main.application.port.out.EloPort
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetPlayerStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val eloPort: EloPort,
    private val cache: StatsQueryCache,
) : GetPlayerStatsUseCase {

    /**
     * LCU lane/role 필드는 커스텀 게임에서 부정확할 수 있습니다.
     * - JUNGLE: neutralMinionsKilled >= 30 으로 실제 정글 여부를 추가 검증
     * - SUPPORT: LCU는 "DUO_SUPPORT" 대신 "SUPPORT" 또는 "NONE" 사용
     * - lane=JUNGLE & neutralMinionsKilled < 30: 오분류 탑라이너 → null(미분류) 처리
     */
    private fun resolvePosition(lane: String?, role: String?, neutralMinionsKilled: Int): String? = when {
        lane == "TOP"                                                          -> "TOP"
        lane == "JUNGLE" && neutralMinionsKilled >= 30                        -> "JUNGLE"
        lane == "MID" || lane == "MIDDLE"                                     -> "MID"
        lane == "BOTTOM" && (role == "DUO_SUPPORT" || role == "SUPPORT")      -> "SUPPORT"
        lane == "BOTTOM"                                                       -> "BOTTOM"
        else                                                                   -> null
    }

    override fun getPlayerStats(riotId: String, mode: String, lane: String?): PlayerDetailStatsResult = cache.getOrCompute("player-stats:$riotId:$mode:$lane") {
        val allElos = eloPort.findAll().sortedByDescending { it.elo }
        val playerElo = allElos.firstOrNull { it.riotId == riotId }
        val eloRank = allElos.indexOfFirst { it.riotId == riotId }.takeIf { it >= 0 }?.plus(1)

        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class Entry(
            val matchId: String, val queueId: Int, val gameCreation: Long, val gameDuration: Int,
            val champion: String, val championId: Int, val win: Boolean,
            val kills: Int, val deaths: Int, val assists: Int,
            val damage: Int, val cs: Int, val gold: Int, val visionScore: Int,
            val position: String?,
            val damageTaken: Int, val objectiveDamage: Int,
            val wardsPlaced: Int, val ccTime: Int, val neutralMinions: Int,
        )

        val entries = matches.flatMap { m ->
            m.participants
                .filter { it.riotId == riotId }
                .map { p ->
                    Entry(
                        matchId = m.matchId, queueId = m.queueId,
                        gameCreation = m.gameCreation, gameDuration = m.gameDuration,
                        champion = p.champion, championId = p.championId, win = p.win,
                        kills = p.kills, deaths = p.deaths, assists = p.assists,
                        damage = p.damage, cs = p.cs, gold = p.gold, visionScore = p.visionScore,
                        position = resolvePosition(p.lane, p.role, p.neutralMinionsKilled),
                        damageTaken = p.totalDamageTaken,
                        objectiveDamage = p.damageDealtToObjectives,
                        wardsPlaced = p.wardsPlaced,
                        ccTime = p.timeCCingOthers,
                        neutralMinions = p.neutralMinionsKilled,
                    )
                }
        }

        val filteredEntries = if (lane != null) entries.filter { it.position == lane } else entries

        if (filteredEntries.isEmpty()) return PlayerDetailStatsResult(
            riotId = riotId, games = 0, wins = 0, losses = 0, winRate = 0,
            avgKills = 0.0, avgDeaths = 0.0, avgAssists = 0.0, kda = 0.0,
            avgDamage = 0, avgCs = 0.0, avgGold = 0, avgVisionScore = 0.0,
            elo = playerElo?.elo ?: 1500.0, eloRank = eloRank,
            championStats = emptyList(), recentMatches = emptyList(), laneStats = emptyList(),
        )

        val games = filteredEntries.size
        val wins  = filteredEntries.count { it.win }
        val kills   = filteredEntries.sumOf { it.kills }
        val deaths  = filteredEntries.sumOf { it.deaths }
        val assists = filteredEntries.sumOf { it.assists }

        fun r1(v: Double) = (v * 10).toInt() / 10.0
        fun r2(v: Double) = (v * 100).toInt() / 100.0
        fun kda(k: Int, d: Int, a: Int) = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()

        val championStats = filteredEntries.groupBy { it.champion }.map { (champ, es) ->
            val cg = es.size; val cw = es.count { it.win }
            val ck = es.sumOf { it.kills }; val cd = es.sumOf { it.deaths }; val ca = es.sumOf { it.assists }
            ChampionStat(
                champion = champ, championId = es.first().championId,
                games = cg, wins = cw, winRate = cw * 100 / cg,
                avgKills = r1(ck.toDouble() / cg), avgDeaths = r1(cd.toDouble() / cg),
                avgAssists = r1(ca.toDouble() / cg), kda = kda(ck, cd, ca),
                avgDamage = es.sumOf { it.damage } / cg,
                avgCs = r1(es.sumOf { it.cs }.toDouble() / cg),
                avgGold = es.sumOf { it.gold } / cg,
            )
        }.sortedByDescending { it.games }

        val recentMatches = filteredEntries.sortedByDescending { it.gameCreation }.take(20).map {
            RecentMatchStat(matchId = it.matchId, champion = it.champion, championId = it.championId,
                win = it.win, kills = it.kills, deaths = it.deaths, assists = it.assists,
                damage = it.damage, cs = it.cs, gold = it.gold,
                gameCreation = it.gameCreation, gameDuration = it.gameDuration, queueId = it.queueId)
        }

        // ── 포지션별 통계 (lane 필터 없이 전체 entries 기준) ──────────────────────────────────────
        val POSITION_ORDER = listOf("TOP", "JUNGLE", "MID", "BOTTOM", "SUPPORT")
        val laneStats = entries
            .filter { it.position != null }
            .groupBy { it.position!! }
            .map { (pos, es) ->
                val pg = es.size; val pw = es.count { it.win }
                val pk = es.sumOf { it.kills }; val pd = es.sumOf { it.deaths }; val pa = es.sumOf { it.assists }
                LaneStat(
                    position = pos,
                    games = pg, wins = pw, winRate = pw * 100 / pg,
                    avgKills  = r1(pk.toDouble() / pg),
                    avgDeaths = r1(pd.toDouble() / pg),
                    avgAssists = r1(pa.toDouble() / pg),
                    kda = kda(pk, pd, pa),
                    avgDamage        = es.sumOf { it.damage } / pg,
                    avgCs            = r1(es.sumOf { it.cs }.toDouble() / pg),
                    avgGold          = es.sumOf { it.gold } / pg,
                    avgVisionScore   = r1(es.sumOf { it.visionScore }.toDouble() / pg),
                    avgDamageTaken   = es.sumOf { it.damageTaken } / pg,
                    avgObjectiveDamage = es.sumOf { it.objectiveDamage } / pg,
                    avgWardsPlaced   = r1(es.sumOf { it.wardsPlaced }.toDouble() / pg),
                    avgCcTime        = r1(es.sumOf { it.ccTime }.toDouble() / pg),
                    avgNeutralMinions = r1(es.sumOf { it.neutralMinions }.toDouble() / pg),
                )
            }
            .sortedBy { POSITION_ORDER.indexOf(it.position).let { i -> if (i == -1) 99 else i } }

        PlayerDetailStatsResult(
            riotId = riotId, games = games, wins = wins, losses = games - wins,
            winRate = wins * 100 / games,
            avgKills   = r1(kills.toDouble() / games),
            avgDeaths  = r1(deaths.toDouble() / games),
            avgAssists = r1(assists.toDouble() / games),
            kda        = kda(kills, deaths, assists),
            avgDamage  = filteredEntries.sumOf { it.damage } / games,
            avgCs      = r1(filteredEntries.sumOf { it.cs }.toDouble() / games),
            avgGold    = filteredEntries.sumOf { it.gold } / games,
            avgVisionScore = r1(filteredEntries.sumOf { it.visionScore }.toDouble() / games),
            elo        = playerElo?.elo ?: 1500.0,
            eloRank    = eloRank,
            championStats = championStats,
            recentMatches = recentMatches,
            laneStats = laneStats,
        )
    }
}
