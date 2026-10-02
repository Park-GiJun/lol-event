package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PlayerStatsCacheJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PlayerStatsCacheJpaRepository : JpaRepository<PlayerStatsCacheJpaEntity, Long> {
    fun findAllByMode(mode: String): List<PlayerStatsCacheJpaEntity>

    fun findByRiotIdAndMode(
        riotId: String,
        mode: String,
    ): PlayerStatsCacheJpaEntity?

    @Modifying
    @Query("DELETE FROM PlayerStatsCacheJpaEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
