package com.gijun.main.infrastructure.adapter.out.persistence.batch.repository

import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.PlayerStatsCacheEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PlayerStatsCacheRepository : JpaRepository<PlayerStatsCacheEntity, Long> {
    fun findAllByMode(mode: String): List<PlayerStatsCacheEntity>
    fun findByRiotIdAndMode(riotId: String, mode: String): PlayerStatsCacheEntity?

    @Modifying
    @Query("DELETE FROM PlayerStatsCacheEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
