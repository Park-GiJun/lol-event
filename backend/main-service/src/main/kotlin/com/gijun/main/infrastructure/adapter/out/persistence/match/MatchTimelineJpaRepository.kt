package com.gijun.main.infrastructure.adapter.out.persistence.match

import com.gijun.main.infrastructure.adapter.out.persistence.match.MatchTimelineJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchTimelineJpaRepository : JpaRepository<MatchTimelineJpaEntity, String> {
    fun findAllByMatchIdIn(matchIds: Collection<String>): List<MatchTimelineJpaEntity>
}
