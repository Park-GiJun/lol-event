package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionItemStatsCacheJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChampionItemStatsCacheJpaRepository : JpaRepository<ChampionItemStatsCacheJpaEntity, Long> {
    fun findAllByChampionAndMode(
        champion: String,
        mode: String,
    ): List<ChampionItemStatsCacheJpaEntity>

    @Modifying
    @Query("DELETE FROM ChampionItemStatsCacheJpaEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
