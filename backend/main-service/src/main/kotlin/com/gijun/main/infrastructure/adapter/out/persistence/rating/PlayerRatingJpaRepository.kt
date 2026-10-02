package com.gijun.main.infrastructure.adapter.out.persistence.rating

import com.gijun.main.infrastructure.adapter.out.persistence.rating.PlayerRatingJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerRatingJpaRepository : JpaRepository<PlayerRatingJpaEntity, Long> {
    fun findByRiotId(riotId: String): PlayerRatingJpaEntity?

    fun findAllByRiotIdIn(riotIds: Collection<String>): List<PlayerRatingJpaEntity>
}
