package com.gijun.main.infrastructure.adapter.out.persistence.batch.repository

import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.ChampionItemStatsCacheEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChampionItemStatsCacheRepository : JpaRepository<ChampionItemStatsCacheEntity, Long> {

    fun findAllByChampionAndMode(champion: String, mode: String): List<ChampionItemStatsCacheEntity>

    @Modifying
    @Query("DELETE FROM ChampionItemStatsCacheEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
