package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionRuneStatsCacheJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChampionRuneStatsCacheJpaRepository : JpaRepository<ChampionRuneStatsCacheJpaEntity, Long> {
    fun findAllByChampionAndMode(
        champion: String,
        mode: String,
    ): List<ChampionRuneStatsCacheJpaEntity>

    @Modifying
    @Query("DELETE FROM ChampionRuneStatsCacheJpaEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
