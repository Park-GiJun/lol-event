package com.gijun.main.infrastructure.adapter.out.persistence.batch.repository

import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.PositionHeatmapCacheEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PositionHeatmapCacheRepository : JpaRepository<PositionHeatmapCacheEntity, Long> {

    /** 히트맵 한 장. 인덱스가 정확히 이 조건을 받는다. */
    fun findAllByModeAndScopeTypeAndScopeKeyAndKind(
        mode: String,
        scopeType: String,
        scopeKey: String,
        kind: String,
    ): List<PositionHeatmapCacheEntity>

    @Modifying
    @Query("DELETE FROM PositionHeatmapCacheEntity e WHERE e.mode = :mode")
    fun deleteAllByMode(mode: String)
}
