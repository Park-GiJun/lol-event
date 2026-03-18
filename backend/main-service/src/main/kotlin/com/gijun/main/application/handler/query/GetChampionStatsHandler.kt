package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionItemStat
import com.gijun.main.application.dto.stats.result.ChampionLaneStat
import com.gijun.main.application.dto.stats.result.ChampionPlayerStat
import com.gijun.main.application.port.`in`.GetChampionStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetChampionStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val statsCachePersistencePort: StatsCachePersistencePort,
) : GetChampionStatsUseCase {

    private fun resolvePosition(lane: String?, role: String?, neutralMinionsKilled: Int): String? = when {
        lane == "TOP"                                                          -> "TOP"
        lane == "JUNGLE" && neutralMinionsKilled >= 30                        -> "JUNGLE"
        lane == "MID" || lane == "MIDDLE"                                     -> "MID"
        lane == "BOTTOM" && (role == "DUO_SUPPORT" || role == "SUPPORT")      -> "SUPPORT"
        lane == "BOTTOM"                                                       -> "BOTTOM"
        else                                                                   -> null
    }

    override fun getChampionStats(champion: String, mode: String): ChampionDetailStats {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        // champion 이름과 일치하는 참가자 수집 (대소문자 무시)
        val pairs = matches.flatMap { m ->
            m.participants
                .filter { it.champion.equals(champion, ignoreCase = true) }
                .map { it to m }
        }

        if (pairs.isEmpty()) return ChampionDetailStats(
            champion = champion, championId = 0,
            totalGames = 0, totalWins = 0, winRate = 0,
            players = emptyList(),
        )

        val championName = pairs.first().first.champion
        val championId   = pairs.first().first.championId
        val totalGames   = pairs.size
        val totalWins    = pairs.count { (p, _) -> p.win }

        fun r1(v: Double) = (v * 10).toInt() / 10.0
        fun r2(v: Double) = (v * 100).toInt() / 100.0
        fun kda(k: Int, d: Int, a: Int) = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()

        val players = pairs
            .groupBy { (p, _) -> p.riotId }
            .map { (riotId, entries) ->
                val gs = entries.size
                val ws = entries.count { (p, _) -> p.win }
                val k  = entries.sumOf { (p, _) -> p.kills }
                val d  = entries.sumOf { (p, _) -> p.deaths }
                val a  = entries.sumOf { (p, _) -> p.assists }
                ChampionPlayerStat(
                    riotId         = riotId,
                    games          = gs,
                    wins           = ws,
                    winRate        = ws * 100 / gs,
                    avgKills       = r1(k.toDouble() / gs),
                    avgDeaths      = r1(d.toDouble() / gs),
                    avgAssists     = r1(a.toDouble() / gs),
                    kda            = kda(k, d, a),
                    avgDamage      = entries.sumOf { (p, _) -> p.damage } / gs,
                    avgCs          = r1(entries.sumOf { (p, _) -> p.cs }.toDouble() / gs),
                    avgGold        = entries.sumOf { (p, _) -> p.gold } / gs,
                    avgVisionScore = r1(entries.sumOf { (p, _) -> p.visionScore }.toDouble() / gs),
                )
            }
            .sortedByDescending { it.games }

        // ── 아이템 통계 — 배치 캐시 우선, 없으면 실시간 계산 ────────
        val cachedItems = statsCachePersistencePort.findChampionItemCacheByChampionAndMode(championName, mode)
        val itemStats: List<ChampionItemStat> = if (cachedItems.isNotEmpty()) {
            cachedItems.map { c ->
                ChampionItemStat(itemId = c.itemId, picks = c.picks, wins = c.wins, winRate = c.winRate)
            }
        } else {
            val TRINKET_IDS = setOf(3340, 3363, 3364, 2052, 2055)
            pairs
                .flatMap { (p, _) ->
                    listOf(p.item0, p.item1, p.item2, p.item3, p.item4, p.item5)
                        .filter { it > 0 && it !in TRINKET_IDS }
                        .map { it to p.win }
                }
                .groupBy { it.first }
                .map { (itemId, entries) ->
                    val iWins = entries.count { it.second }
                    ChampionItemStat(
                        itemId  = itemId,
                        picks   = entries.size,
                        wins    = iWins,
                        winRate = if (entries.isNotEmpty()) iWins * 100 / entries.size else 0,
                    )
                }
                .sortedByDescending { it.picks }
                .take(6)
        }

        // ── 라인별 통계 ──────────────────────────────────────────────
        val POSITION_ORDER = listOf("TOP", "JUNGLE", "MID", "BOTTOM", "SUPPORT")
        val laneStats = pairs
            .mapNotNull { (p, _) ->
                val pos = resolvePosition(p.lane, p.role, p.neutralMinionsKilled) ?: return@mapNotNull null
                pos to p
            }
            .groupBy { it.first }
            .map { (pos, entries) ->
                val ps = entries.map { it.second }
                val lg = ps.size; val lw = ps.count { it.win }
                val lk = ps.sumOf { it.kills }; val ld = ps.sumOf { it.deaths }; val la = ps.sumOf { it.assists }
                ChampionLaneStat(
                    position   = pos,
                    games      = lg, wins = lw, winRate = lw * 100 / lg,
                    avgKills   = r1(lk.toDouble() / lg),
                    avgDeaths  = r1(ld.toDouble() / lg),
                    avgAssists = r1(la.toDouble() / lg),
                    kda        = kda(lk, ld, la),
                    avgDamage  = ps.sumOf { it.damage } / lg,
                    avgCs      = r1(ps.sumOf { it.cs }.toDouble() / lg),
                    avgGold    = ps.sumOf { it.gold } / lg,
                )
            }
            .sortedBy { POSITION_ORDER.indexOf(it.position).let { i -> if (i == -1) 99 else i } }

        return ChampionDetailStats(
            champion   = championName,
            championId = championId,
            totalGames = totalGames,
            totalWins  = totalWins,
            winRate    = totalWins * 100 / totalGames,
            players    = players,
            itemStats  = itemStats,
            laneStats  = laneStats,
        )
    }
}
