package com.gijun.main.application.port.out

import com.gijun.main.domain.model.match.Match

interface MatchPersistencePort {
    fun existsByMatchId(matchId: String): Boolean
    fun findByMatchId(matchId: String): Match?
    fun save(match: Match): Match
    fun findAllWithParticipants(queueIds: List<Int>): List<Match>
    fun deleteByMatchId(matchId: String)
    fun countByQueueIds(queueIds: List<Int>): Long
}
