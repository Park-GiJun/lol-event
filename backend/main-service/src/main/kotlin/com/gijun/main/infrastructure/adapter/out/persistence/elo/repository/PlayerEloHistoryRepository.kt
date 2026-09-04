package com.gijun.main.infrastructure.adapter.out.persistence.elo.repository

import com.gijun.main.infrastructure.adapter.out.persistence.elo.entity.PlayerEloHistoryEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PlayerEloHistoryRepository : JpaRepository<PlayerEloHistoryEntity, Long> {
    fun findByRiotIdOrderByGameCreationDesc(riotId: String, pageable: Pageable): List<PlayerEloHistoryEntity>

    fun existsByMatchId(matchId: String): Boolean

    @Query("SELECT MAX(h.gameCreation) FROM PlayerEloHistoryEntity h")
    fun findLatestGameCreation(): Long?
}
