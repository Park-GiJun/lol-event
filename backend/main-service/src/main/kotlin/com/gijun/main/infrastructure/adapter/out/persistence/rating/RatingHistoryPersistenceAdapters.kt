package com.gijun.main.infrastructure.adapter.out.persistence.rating

import com.gijun.main.application.port.out.persistence.RatingHistoryCommandPersistencePort
import com.gijun.main.application.port.out.persistence.RatingHistoryQueryPersistencePort
import com.gijun.main.domain.rating.model.RatingHistoryModel
import com.gijun.main.infrastructure.adapter.out.persistence.rating.RatingHistoryJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.rating.RatingHistoryJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RatingHistoryQueryPersistenceAdapter(
    private val repo: RatingHistoryJpaRepository,
) : RatingHistoryQueryPersistencePort {
    override fun findByRiotId(
        riotId: String,
        limit: Int,
    ): List<RatingHistoryModel> = repo.findByRiotIdOrderByGameCreationDesc(riotId, PageRequest.of(0, limit)).map { it.toModel() }

    override fun existsByMatchId(matchId: String): Boolean = repo.existsByMatchId(matchId)

    override fun findLatestGameCreation(): Long? = repo.findLatestGameCreation()
}

@Component
class RatingHistoryCommandPersistenceAdapter(
    private val repo: RatingHistoryJpaRepository,
) : RatingHistoryCommandPersistencePort {
    @Transactional
    override fun saveAll(histories: List<RatingHistoryModel>) {
        if (histories.isEmpty()) return
        repo.saveAll(histories.map { RatingHistoryJpaEntity.from(it) })
    }

    /**
     * 재집계는 전체 삭제 후 같은 (riot_id, match_id) 를 다시 넣는다. 유니크 제약이 걸려 있어서
     * 삭제가 INSERT 보다 먼저 DB 에 도달해야 한다. 영속성 컨텍스트에 맡기면 flush 순서가
     * 뒤집힐 수 있으므로 즉시 실행되는 일괄 삭제를 쓴다.
     */
    @Transactional
    override fun deleteAll() = repo.deleteAllInBatch()
}
