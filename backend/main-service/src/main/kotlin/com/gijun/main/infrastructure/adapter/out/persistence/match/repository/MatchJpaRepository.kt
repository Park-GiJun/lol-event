package com.gijun.main.infrastructure.adapter.out.persistence.match.repository

import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchEntity
import com.gijun.main.domain.model.match.LaneMethod
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MatchJpaRepository : JpaRepository<MatchEntity, Long> {
    fun existsByMatchId(matchId: String): Boolean
    fun findByMatchId(matchId: String): MatchEntity?
    fun deleteByMatchId(matchId: String)

    @Query("SELECT DISTINCT m FROM MatchEntity m LEFT JOIN FETCH m.participants WHERE m.queueId IN :queueIds ORDER BY m.gameCreation DESC")
    fun findAllWithParticipantsByQueueIdIn(queueIds: List<Int>): List<MatchEntity>

    fun countByQueueIdIn(queueIds: List<Int>): Long

    /**
     * 페이지에 해당하는 matchId 만 먼저 뽑는다.
     * JOIN FETCH 와 페이징을 한 쿼리에 섞으면 JPA 가 전체를 읽고 메모리에서 자르기 때문에
     * (HHH000104) 페이징 효과가 사라진다. 그래서 2단계로 나눈다.
     */
    @Query("SELECT m.matchId FROM MatchEntity m WHERE m.queueId IN :queueIds ORDER BY m.gameCreation DESC")
    fun findMatchIdsByQueueIdIn(queueIds: List<Int>, pageable: Pageable): List<String>

    @Query("SELECT DISTINCT m FROM MatchEntity m LEFT JOIN FETCH m.participants WHERE m.matchId IN :matchIds ORDER BY m.gameCreation DESC")
    fun findAllWithParticipantsByMatchIdIn(matchIds: List<String>): List<MatchEntity>

    @Query("SELECT DISTINCT m FROM MatchEntity m LEFT JOIN FETCH m.participants ORDER BY m.gameCreation ASC")
    fun findAllWithParticipantsOrderedByGameCreation(): List<MatchEntity>

    @Query("SELECT MIN(m.gameCreation) FROM MatchEntity m WHERE m.queueId IN :queueIds")
    fun findFirstGameCreation(queueIds: List<Int>): Long?

    @Query("SELECT MAX(m.gameCreation) FROM MatchEntity m WHERE m.queueId IN :queueIds")
    fun findLastGameCreation(queueIds: List<Int>): Long?

    /**
     * 같은 판정 방법으로 바뀌는 경기들을 한 번에 갱신 (재집계용).
     * 방법이 두세 종류뿐이라 UPDATE 도 그만큼으로 끝난다.
     */
    @Modifying
    @Query("UPDATE MatchEntity m SET m.laneMethod = :method WHERE m.matchId IN :matchIds")
    fun updateLaneMethodIn(@Param("matchIds") matchIds: Collection<String>, @Param("method") method: LaneMethod): Int

    /** 경기에 한 번이라도 등장한 인원. 등록 멤버가 아니라 실제 참가자 기준이다. */
    @Query("SELECT COUNT(DISTINCT p.riotId) FROM MatchEntity m JOIN m.participants p WHERE m.queueId IN :queueIds")
    fun countDistinctPlayers(queueIds: List<Int>): Long
}
