package com.gijun.main.infrastructure.adapter.out.persistence.batch.repository

import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.ChampionStatsCacheEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChampionStatsCacheRepository : JpaRepository<ChampionStatsCacheEntity, Long> {
    fun findAllByMode(mode: String): List<ChampionStatsCacheEntity>

    @Modifying
    @Query("DELETE FROM ChampionStatsCacheEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
