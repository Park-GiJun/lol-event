package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import com.gijun.main.application.port.out.persistence.ChampionItemStatsCache
import com.gijun.main.application.port.out.persistence.ChampionRuneStatsCache
import com.gijun.main.application.port.out.persistence.ChampionTimelineStatsCache
import com.gijun.main.application.port.out.persistence.HeatmapCell
import com.gijun.main.application.port.out.persistence.PlayerStatsCache
import com.gijun.main.application.port.out.persistence.PlayerTimelineStatsCache
import com.gijun.main.application.port.out.persistence.StatsCacheQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionItemStatsCacheJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionRuneStatsCacheJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionTimelineStatsCacheJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PlayerStatsCacheJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PlayerTimelineStatsCacheJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PositionHeatmapCacheJpaRepository
import com.gijun.main.shared.domain.vo.RiotId
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

@Component
class StatsCacheQueryPersistenceAdapter(
    private val playerStatsCacheRepository: PlayerStatsCacheJpaRepository,
    private val championItemStatsCacheRepository: ChampionItemStatsCacheJpaRepository,
    private val championRuneStatsCacheRepository: ChampionRuneStatsCacheJpaRepository,
    private val playerTimelineStatsCacheRepository: PlayerTimelineStatsCacheJpaRepository,
    private val championTimelineStatsCacheRepository: ChampionTimelineStatsCacheJpaRepository,
    private val positionHeatmapCacheRepository: PositionHeatmapCacheJpaRepository,
    private val championStatsCacheRepository: ChampionStatsCacheJpaRepository,
) : StatsCacheQueryPersistencePort {
    override fun countPlayerSnapshots(): Long = playerStatsCacheRepository.count()

    override fun countChampionSnapshots(): Long = championStatsCacheRepository.count()

    override fun countChampionItemSnapshots(): Long = championItemStatsCacheRepository.count()

    override fun findLastPlayerAggregatedAt(mode: GameMode): LocalDateTime? =
        playerStatsCacheRepository.findAllByMode(mode.key).maxOfOrNull { it.aggregatedAt }

    override fun findPlayerCacheByMode(mode: GameMode): List<PlayerStatsCache> =
        playerStatsCacheRepository.findAllByMode(mode.key).map { e ->
            PlayerStatsCache(
                riotId = e.riotId,
                games = e.games,
                wins = e.wins,
                losses = e.losses,
                winRate = e.winRate,
                avgKills = e.avgKills,
                avgDeaths = e.avgDeaths,
                avgAssists = e.avgAssists,
                kda = e.kda,
                avgDamage = e.avgDamage,
                avgCs = e.avgCs,
                avgGold = e.avgGold,
                avgVisionScore = e.avgVisionScore,
                topChampion = e.topChampion,
            )
        }

    override fun findChampionItemCacheByChampionAndMode(
        champion: String,
        mode: GameMode,
    ): List<ChampionItemStatsCache> =
        championItemStatsCacheRepository
            .findAllByChampionAndMode(champion, mode.key)
            .sortedByDescending { it.picks }
            .take(6)
            .map { e ->
                ChampionItemStatsCache(
                    itemId = e.itemId,
                    picks = e.picks,
                    wins = e.wins,
                    winRate = e.winRate,
                )
            }

    override fun findChampionRuneCacheByChampionAndMode(
        champion: String,
        mode: GameMode,
    ): List<ChampionRuneStatsCache> =
        championRuneStatsCacheRepository
            .findAllByChampionAndMode(champion, mode.key)
            .sortedByDescending { it.picks }
            // 룬 조합은 아이템 칸보다 가짓수가 적다. 상위 5개면 화면에 다 담긴다.
            .take(5)
            .map { e ->
                ChampionRuneStatsCache(
                    keystone = e.keystone,
                    primaryStyle = e.primaryStyle,
                    subStyle = e.subStyle,
                    picks = e.picks,
                    wins = e.wins,
                    winRate = e.winRate,
                )
            }

    // ────────── 타임라인 파생 ──────────

    override fun findPlayerTimelineCache(
        riotId: RiotId,
        mode: GameMode,
    ): PlayerTimelineStatsCache? =
        playerTimelineStatsCacheRepository.findByRiotIdAndMode(riotId.value, mode.key)?.let { e ->
            PlayerTimelineStatsCache(
                riotId = e.riotId,
                games = e.games,
                framesSampled = e.framesSampled,
                laneShareRate = e.laneShareRate?.toDouble(),
                roamRate = e.roamRate?.toDouble(),
                enemyHalfRate = e.enemyHalfRate.toDouble(),
                counterJungleRate = e.counterJungleRate.toDouble(),
                teamfights = e.teamfights,
                teamfightKills = e.teamfightKills,
                teamfightDeaths = e.teamfightDeaths,
                aggregatedAt =
                    e.aggregatedAt
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
            )
        }

    /**
     * 챔피언 쪽은 합계로 저장돼 있어서 **여기서 나눈다.**
     * 평균으로 저장하면 포지션별 행을 챔피언 총합으로 롤업할 때 가중치를 잃는다.
     */
    override fun findChampionTimelineCache(
        champion: String,
        mode: GameMode,
    ): List<ChampionTimelineStatsCache> =
        championTimelineStatsCacheRepository
            .findAllByChampionAndMode(champion, mode.key)
            .sortedByDescending { it.games }
            .map { e ->
                ChampionTimelineStatsCache(
                    champion = e.champion,
                    position = e.position,
                    games = e.games,
                    framesSampled = e.framesSampled,
                    laneShareRate = rate(e.sumLaneFrames, e.lanePhaseFrames),
                    enemyHalfRate = rate(e.sumEnemyHalf, e.framesSampled) ?: 0.0,
                    aggregatedAt =
                        e.aggregatedAt
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli(),
                )
            }

    override fun findHeatmap(
        mode: GameMode,
        scopeType: String,
        scopeKey: String,
        kind: String,
    ): List<HeatmapCell> =
        positionHeatmapCacheRepository
            .findAllByModeAndScopeTypeAndScopeKeyAndKind(mode.key, scopeType, scopeKey, kind)
            .map { HeatmapCell(phase = it.phase, gridX = it.gridX, gridY = it.gridY, count = it.count) }

    private fun rate(
        part: Long,
        total: Int,
    ): Double? = if (total <= 0) null else ((part * 1_000.0 / total).roundToInt() / 10.0)
}
