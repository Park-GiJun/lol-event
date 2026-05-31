package com.gijun.main.application.port.out

import com.gijun.main.domain.model.match.Match

interface MatchPersistencePort {
    fun existsByMatchId(matchId: String): Boolean
    fun findByMatchId(matchId: String): Match?
    fun save(match: Match): Match
    fun findAllWithParticipants(queueIds: List<Int>): List<Match>
    fun deleteByMatchId(matchId: String)
    fun countByQueueIds(queueIds: List<Int>): Long
    fun findAllOrderedByGameCreation(): List<Match>

    /** 참가자들의 assignedPosition 만 일괄 갱신 (포지션 백필용). key = participantId, value = Position.name */
    fun updateAssignedPositions(updates: Map<Long, String>)
}
