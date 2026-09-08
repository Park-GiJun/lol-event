package com.gijun.main.infrastructure.adapter.out.persistence.match.repository

import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchParticipantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MatchParticipantJpaRepository : JpaRepository<MatchParticipantEntity, Long> {

    /**
     * 같은 포지션으로 바뀌는 참가자들을 한 번에 갱신 (포지션 백필용 — 행 재삽입/ID 변경 없음).
     *
     * 예전에는 참가자 한 명당 UPDATE 한 방이었다. 백필은 매치 전체를 훑기 때문에
     * 참가자 1,500명이면 한 트랜잭션 안에서 왕복이 1,500번 생겼다.
     * 배정 결과는 포지션 5종뿐이라 포지션별로 묶으면 UPDATE 5방으로 끝난다.
     */
    @Modifying
    @Query("UPDATE MatchParticipantEntity p SET p.assignedPosition = :pos WHERE p.id IN :ids")
    fun updateAssignedPositionIn(@Param("ids") ids: Collection<Long>, @Param("pos") pos: String): Int
}
