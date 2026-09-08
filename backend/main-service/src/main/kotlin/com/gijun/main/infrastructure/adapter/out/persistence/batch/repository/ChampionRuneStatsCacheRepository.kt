package com.gijun.main.infrastructure.adapter.out.persistence.batch.repository

import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.ChampionRuneStatsCacheEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChampionRuneStatsCacheRepository : JpaRepository<ChampionRuneStatsCacheEntity, Long> {

    fun findAllByChampionAndMode(champion: String, mode: String): List<ChampionRuneStatsCacheEntity>

    @Modifying
    @Query("DELETE FROM ChampionRuneStatsCacheEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
