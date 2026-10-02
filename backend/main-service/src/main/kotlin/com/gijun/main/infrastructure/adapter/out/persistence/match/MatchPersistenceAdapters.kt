package com.gijun.main.infrastructure.adapter.out.persistence.match

import com.gijun.main.application.port.out.persistence.MatchCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MatchPeriodSummary
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.PositionCount
import com.gijun.main.domain.match.enums.LaneMethod
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.infrastructure.adapter.out.persistence.match.MatchJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.MatchJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.match.MatchParticipantJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.MatchParticipantJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.match.MatchTeamJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.MatchTimelineJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.match.MatchTimelineJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MatchQueryPersistenceAdapter(
    private val repo: MatchJpaRepository,
    private val participantRepo: MatchParticipantJpaRepository,
    private val timelineRepo: MatchTimelineJpaRepository,
) : MatchQueryPersistencePort {
    override fun existsByMatchId(matchId: String): Boolean = repo.existsByMatchId(matchId)

    override fun findByMatchId(matchId: String): MatchModel? = repo.findByMatchId(matchId)?.toModel()

    override fun findAllWithParticipants(queueIds: List<Int>): List<MatchModel> =
        repo.findAllWithParticipantsByQueueIdIn(queueIds).map { it.toModel() }

    override fun findPageWithParticipants(
        queueIds: List<Int>,
        page: Int,
        size: Int,
    ): List<MatchModel> {
        val matchIds = repo.findMatchIdsByQueueIdIn(queueIds, PageRequest.of(page, size))
        if (matchIds.isEmpty()) return emptyList()
        return repo.findAllWithParticipantsByMatchIdIn(matchIds).map { it.toModel() }
    }

    override fun findPeriodSummary(queueIds: List<Int>): MatchPeriodSummary =
        MatchPeriodSummary(
            firstMatchAt = repo.findFirstGameCreation(queueIds),
            lastMatchAt = repo.findLastGameCreation(queueIds),
            totalMatches = repo.countByQueueIdIn(queueIds),
            playerCount = repo.countDistinctPlayers(queueIds),
        )

    override fun countByQueueIds(queueIds: List<Int>): Long = repo.countByQueueIdIn(queueIds)

    override fun findAllOrderedByGameCreation(): List<MatchModel> = repo.findAllWithParticipantsOrderedByGameCreation().map { it.toModel() }

    override fun findInPeriodWithParticipants(
        queueIds: List<Int>,
        fromMs: Long,
        untilMs: Long,
    ): List<MatchModel> = repo.findAllWithParticipantsInPeriod(queueIds, fromMs, untilMs).map { it.toModel() }

    override fun findTimelineRaw(matchIds: Collection<String>): Map<String, String> =
        matchIds
            .chunked(IN_CLAUSE_CHUNK)
            .flatMap { timelineRepo.findAllByMatchIdIn(it) }
            .associate { it.matchId to it.raw }

    override fun findPositionCounts(): List<PositionCount> = participantRepo.findPositionCounts()

    private companion object {
        const val IN_CLAUSE_CHUNK = 1_000
    }
}

@Component
class MatchCommandPersistenceAdapter(
    private val repo: MatchJpaRepository,
    private val participantRepo: MatchParticipantJpaRepository,
    private val timelineRepo: MatchTimelineJpaRepository,
) : MatchCommandPersistencePort {
    @Transactional
    override fun save(match: MatchModel): MatchModel {
        val entity = repo.findByMatchId(match.matchId) ?: MatchJpaEntity.from(match)

        entity.participants.clear()
        entity.teams.clear()

        match.participants.forEach { entity.participants.add(MatchParticipantJpaEntity.from(it, entity)) }
        match.teams.forEach { entity.teams.add(MatchTeamJpaEntity.from(it, entity)) }

        return repo.save(entity).toModel()
    }

    @Transactional
    override fun deleteByMatchId(matchId: String) = repo.deleteByMatchId(matchId)

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

    @Transactional
    override fun saveTimelineRaw(
        matchId: String,
        raw: String,
    ) {
        timelineRepo.save(MatchTimelineJpaEntity(matchId = matchId, raw = raw))
    }

    @Transactional
    override fun updateLaneMethods(updates: Map<String, LaneMethod>) {
        updates.entries
            .groupBy({ it.value }, { it.key })
            .forEach { (method, ids) ->
                ids.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
                    repo.updateLaneMethodIn(chunk, method)
                }
            }
    }

    private companion object {
        const val IN_CLAUSE_CHUNK = 1_000
    }
}
