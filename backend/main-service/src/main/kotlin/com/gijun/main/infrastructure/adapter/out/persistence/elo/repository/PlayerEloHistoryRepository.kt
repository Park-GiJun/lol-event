package com.gijun.main.infrastructure.adapter.out.persistence.elo.repository

import com.gijun.main.infrastructure.adapter.out.persistence.elo.entity.PlayerEloHistoryEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerEloHistoryRepository : JpaRepository<PlayerEloHistoryEntity, Long> {
    fun findByRiotIdOrderByGameCreationDesc(riotId: String, pageable: Pageable): List<PlayerEloHistoryEntity>
}
