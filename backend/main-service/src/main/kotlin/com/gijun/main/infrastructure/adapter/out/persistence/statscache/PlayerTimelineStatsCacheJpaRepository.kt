package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PlayerTimelineStatsCacheJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PlayerTimelineStatsCacheJpaRepository : JpaRepository<PlayerTimelineStatsCacheJpaEntity, Long> {
    fun findAllByMode(mode: String): List<PlayerTimelineStatsCacheJpaEntity>

    fun findByRiotIdAndMode(
        riotId: String,
        mode: String,
    ): PlayerTimelineStatsCacheJpaEntity?

    @Modifying
    @Query("DELETE FROM PlayerTimelineStatsCacheJpaEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
