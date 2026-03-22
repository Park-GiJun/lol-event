package com.gijun.main.infrastructure.adapter.out.persistence.batch

import com.gijun.main.application.port.out.ChampionItemStatsCache
import com.gijun.main.application.port.out.PlayerStatsCache
import com.gijun.main.application.port.out.StatsCachePersistencePort
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.ChampionItemStatsCacheRepository
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.PlayerStatsCacheRepository
import org.springframework.stereotype.Component

@Component
class StatsCacheAdapter(
    private val playerStatsCacheRepository: PlayerStatsCacheRepository,
    private val championItemStatsCacheRepository: ChampionItemStatsCacheRepository,
) : StatsCachePersistencePort {

    override fun findPlayerCacheByMode(mode: String): List<PlayerStatsCache> =
        playerStatsCacheRepository.findAllByMode(mode).map { e ->
            PlayerStatsCache(
                riotId         = e.riotId,
                games          = e.games,
                wins           = e.wins,
                losses         = e.losses,
                winRate        = e.winRate,
                avgKills       = e.avgKills,
                avgDeaths      = e.avgDeaths,
                avgAssists     = e.avgAssists,
                kda            = e.kda,
                avgDamage      = e.avgDamage,
                avgCs          = e.avgCs,
                avgGold        = e.avgGold,
                avgVisionScore = e.avgVisionScore,
                topChampion    = e.topChampion,
            )
        }

    override fun findChampionItemCacheByChampionAndMode(
        champion: String, mode: String,
    ): List<ChampionItemStatsCache> =
        championItemStatsCacheRepository
            .findAllByChampionAndMode(champion, mode)
            .sortedByDescending { it.picks }
            .take(6)
            .map { e ->
                ChampionItemStatsCache(
                    itemId  = e.itemId,
                    picks   = e.picks,
                    wins    = e.wins,
                    winRate = e.winRate,
                )
            }
}
