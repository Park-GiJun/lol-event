package com.gijun.main.infrastructure.adapter.out.persistence.match.repository

import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchTimelineEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchTimelineRepository : JpaRepository<MatchTimelineEntity, String> {
    fun findAllByMatchIdIn(matchIds: Collection<String>): List<MatchTimelineEntity>
}
