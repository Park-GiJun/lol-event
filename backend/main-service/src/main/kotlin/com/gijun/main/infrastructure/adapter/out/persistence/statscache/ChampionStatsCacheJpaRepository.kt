package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionStatsCacheJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChampionStatsCacheJpaRepository : JpaRepository<ChampionStatsCacheJpaEntity, Long> {
    fun findAllByMode(mode: String): List<ChampionStatsCacheJpaEntity>

    @Modifying
    @Query("DELETE FROM ChampionStatsCacheJpaEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
