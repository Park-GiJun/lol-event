package com.gijun.main.infrastructure.adapter.out.persistence.match.repository

import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchParticipantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MatchParticipantJpaRepository : JpaRepository<MatchParticipantEntity, Long> {

    /** 단일 참가자의 assignedPosition 만 갱신 (포지션 백필용 — 참가자 행 재삽입/ID 변경 없음). */
    @Modifying
    @Query("UPDATE MatchParticipantEntity p SET p.assignedPosition = :pos WHERE p.id = :id")
    fun updateAssignedPosition(@Param("id") id: Long, @Param("pos") pos: String): Int
}
