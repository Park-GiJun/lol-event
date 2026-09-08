package com.gijun.main.infrastructure.adapter.out.persistence.match.adapter

import com.gijun.main.application.port.out.MatchPeriodSummary
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.model.match.Match
import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchParticipantEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.entity.MatchTeamEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.repository.MatchJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.match.repository.MatchParticipantJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MatchPersistenceAdapter(
    private val repo: MatchJpaRepository,
    private val participantRepo: MatchParticipantJpaRepository,
) : MatchPersistencePort {

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

    override fun findPageWithParticipants(queueIds: List<Int>, page: Int, size: Int): List<Match> {
        val matchIds = repo.findMatchIdsByQueueIdIn(queueIds, PageRequest.of(page, size))
        if (matchIds.isEmpty()) return emptyList()
        return repo.findAllWithParticipantsByMatchIdIn(matchIds).map { it.toDomain() }
    }

    override fun findPeriodSummary(queueIds: List<Int>): MatchPeriodSummary = MatchPeriodSummary(
        firstMatchAt = repo.findFirstGameCreation(queueIds),
        lastMatchAt = repo.findLastGameCreation(queueIds),
        totalMatches = repo.countByQueueIdIn(queueIds),
        playerCount = repo.countDistinctPlayers(queueIds),
    )

    @Transactional
    override fun deleteByMatchId(matchId: String) = repo.deleteByMatchId(matchId)
    override fun countByQueueIds(queueIds: List<Int>): Long = repo.countByQueueIdIn(queueIds)

    override fun findAllOrderedByGameCreation(): List<Match> =
        repo.findAllWithParticipantsOrderedByGameCreation().map { it.toDomain() }

    @Transactional
    override fun updateAssignedPositions(updates: Map<Long, String>) {
        // 포지션별로 묶어 UPDATE 를 5방 이내로 줄인다. IN 목록이 너무 길면 파라미터 한도에
        // 걸리는 DB 가 있어 청크로 나눈다.
        updates.entries
            .groupBy({ it.value }, { it.key })
            .forEach { (pos, ids) ->
                ids.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
                    participantRepo.updateAssignedPositionIn(chunk, pos)
                }
            }
    }

    private companion object {
        const val IN_CLAUSE_CHUNK = 1_000
    }
}
