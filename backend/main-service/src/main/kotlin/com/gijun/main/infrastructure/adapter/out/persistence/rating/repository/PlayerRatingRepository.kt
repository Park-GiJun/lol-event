package com.gijun.main.infrastructure.adapter.out.persistence.rating.repository

import com.gijun.main.infrastructure.adapter.out.persistence.rating.entity.PlayerRatingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerRatingRepository : JpaRepository<PlayerRatingEntity, Long> {
    fun findByRiotId(riotId: String): PlayerRatingEntity?
    fun findAllByRiotIdIn(riotIds: Collection<String>): List<PlayerRatingEntity>
}
