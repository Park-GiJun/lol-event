package com.gijun.main.infrastructure.adapter.out.persistence.batch.repository

import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.PlayerTimelineStatsCacheEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PlayerTimelineStatsCacheRepository : JpaRepository<PlayerTimelineStatsCacheEntity, Long> {
    fun findAllByMode(mode: String): List<PlayerTimelineStatsCacheEntity>

    fun findByRiotIdAndMode(
        riotId: String,
        mode: String,
    ): PlayerTimelineStatsCacheEntity?

    @Modifying
    @Query("DELETE FROM PlayerTimelineStatsCacheEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
