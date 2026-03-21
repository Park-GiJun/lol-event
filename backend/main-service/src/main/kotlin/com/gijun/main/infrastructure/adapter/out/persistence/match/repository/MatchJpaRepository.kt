package com.gijun.main.infrastructure.adapter.out.persistence.match.repository

import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MatchJpaRepository : JpaRepository<MatchEntity, Long> {
    fun existsByMatchId(matchId: String): Boolean
    fun findByMatchId(matchId: String): MatchEntity?
    fun deleteByMatchId(matchId: String)

    @Query("SELECT DISTINCT m FROM MatchEntity m LEFT JOIN FETCH m.participants WHERE m.queueId IN :queueIds ORDER BY m.gameCreation DESC")
    fun findAllWithParticipantsByQueueIdIn(queueIds: List<Int>): List<MatchEntity>

    fun countByQueueIdIn(queueIds: List<Int>): Long

    @Query("SELECT DISTINCT m FROM MatchEntity m LEFT JOIN FETCH m.participants ORDER BY m.gameCreation ASC")
    fun findAllWithParticipantsOrderedByGameCreation(): List<MatchEntity>
}
