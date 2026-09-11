package com.gijun.main.infrastructure.adapter.out.persistence.rating.repository

import com.gijun.main.infrastructure.adapter.out.persistence.rating.entity.RatingHistoryEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RatingHistoryRepository : JpaRepository<RatingHistoryEntity, Long> {
    fun findByRiotIdOrderByGameCreationDesc(riotId: String, pageable: Pageable): List<RatingHistoryEntity>
    fun existsByMatchId(matchId: String): Boolean

    @Query("SELECT MAX(h.gameCreation) FROM RatingHistoryEntity h")
    fun findLatestGameCreation(): Long?
}
