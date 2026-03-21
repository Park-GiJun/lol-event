package com.gijun.main.infrastructure.adapter.out.persistence.elo.repository

import com.gijun.main.infrastructure.adapter.out.persistence.elo.entity.PlayerEloEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerEloRepository : JpaRepository<PlayerEloEntity, Long> {
    fun findByRiotId(riotId: String): PlayerEloEntity?
    fun findAllByRiotIdIn(riotIds: List<String>): List<PlayerEloEntity>
}
