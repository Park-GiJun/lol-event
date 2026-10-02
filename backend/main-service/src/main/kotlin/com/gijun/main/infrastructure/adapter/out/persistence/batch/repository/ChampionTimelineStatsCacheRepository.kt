package com.gijun.main.infrastructure.adapter.out.persistence.batch.repository

import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.ChampionTimelineStatsCacheEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChampionTimelineStatsCacheRepository : JpaRepository<ChampionTimelineStatsCacheEntity, Long> {
    fun findAllByMode(mode: String): List<ChampionTimelineStatsCacheEntity>

    fun findAllByChampionAndMode(
        champion: String,
        mode: String,
    ): List<ChampionTimelineStatsCacheEntity>

    @Modifying
    @Query("DELETE FROM ChampionTimelineStatsCacheEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
