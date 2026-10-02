package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionTimelineStatsCacheJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChampionTimelineStatsCacheJpaRepository : JpaRepository<ChampionTimelineStatsCacheJpaEntity, Long> {
    fun findAllByMode(mode: String): List<ChampionTimelineStatsCacheJpaEntity>

    fun findAllByChampionAndMode(
        champion: String,
        mode: String,
    ): List<ChampionTimelineStatsCacheJpaEntity>

    @Modifying
    @Query("DELETE FROM ChampionTimelineStatsCacheJpaEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
