package com.gijun.main.infrastructure.adapter.out.persistence.rating

import com.gijun.main.infrastructure.adapter.out.persistence.rating.RatingHistoryJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RatingHistoryJpaRepository : JpaRepository<RatingHistoryJpaEntity, Long> {
    fun findByRiotIdOrderByGameCreationDesc(
        riotId: String,
        pageable: Pageable,
    ): List<RatingHistoryJpaEntity>

    fun existsByMatchId(matchId: String): Boolean

    @Query("SELECT MAX(h.gameCreation) FROM RatingHistoryJpaEntity h")
    fun findLatestGameCreation(): Long?
}
