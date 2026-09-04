package com.gijun.main.application.port.out

import com.gijun.main.domain.model.match.Match

interface MatchPersistencePort {
    fun existsByMatchId(matchId: String): Boolean
    fun findByMatchId(matchId: String): Match?
    fun save(match: Match): Match
    fun findAllWithParticipants(queueIds: List<Int>): List<Match>

    /** 최신순 한 페이지만 참가자까지 채워서 반환한다. 목록 화면 전용. */
    fun findPageWithParticipants(queueIds: List<Int>, page: Int, size: Int): List<Match>

    /** 기록 기간과 등장 인원. 전체 경기를 로드하지 않고 집계 쿼리로 센다. */
    fun findPeriodSummary(queueIds: List<Int>): MatchPeriodSummary
    fun deleteByMatchId(matchId: String)
    fun countByQueueIds(queueIds: List<Int>): Long
    fun findAllOrderedByGameCreation(): List<Match>

    /** 참가자들의 assignedPosition 만 일괄 갱신 (포지션 백필용). key = participantId, value = Position.name */
    fun updateAssignedPositions(updates: Map<Long, String>)
}

data class MatchPeriodSummary(
    val firstMatchAt: Long?,
    val lastMatchAt: Long?,
    val totalMatches: Long,
    val playerCount: Long,
)
