package com.gijun.main.infrastructure.adapter.out.persistence.match.adapter

import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.model.match.Match
import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchParticipantEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchTeamEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.repository.MatchJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MatchPersistenceAdapter(private val repo: MatchJpaRepository) : MatchPersistencePort {

    @Transactional
    override fun save(match: Match): Match {
        val entity = repo.findByMatchId(match.matchId) ?: MatchEntity.from(match)

        entity.participants.clear()
        entity.teams.clear()

        match.participants.forEach { entity.participants.add(MatchParticipantEntity.from(it, entity)) }
        match.teams.forEach { entity.teams.add(MatchTeamEntity.from(it, entity)) }

        return repo.save(entity).toDomain()
    }

    override fun existsByMatchId(matchId: String): Boolean = repo.existsByMatchId(matchId)
    override fun findByMatchId(matchId: String): Match? = repo.findByMatchId(matchId)?.toDomain()
    override fun findAllWithParticipants(queueIds: List<Int>): List<Match> =
        repo.findAllWithParticipantsByQueueIdIn(queueIds).map { it.toDomain() }

    @Transactional
    override fun deleteByMatchId(matchId: String) = repo.deleteByMatchId(matchId)
    override fun countByQueueIds(queueIds: List<Int>): Long = repo.countByQueueIdIn(queueIds)
}
