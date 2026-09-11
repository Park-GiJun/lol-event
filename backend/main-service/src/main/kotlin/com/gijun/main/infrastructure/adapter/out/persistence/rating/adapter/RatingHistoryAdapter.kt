package com.gijun.main.infrastructure.adapter.out.persistence.rating.adapter

import com.gijun.main.application.port.out.RatingHistoryPort
import com.gijun.main.domain.model.rating.RatingHistory
import com.gijun.main.infrastructure.adapter.out.persistence.rating.entity.RatingHistoryEntity
import com.gijun.main.infrastructure.adapter.out.persistence.rating.repository.RatingHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RatingHistoryAdapter(private val repo: RatingHistoryRepository) : RatingHistoryPort {

    @Transactional
    override fun saveAll(histories: List<RatingHistory>) {
        if (histories.isEmpty()) return
        repo.saveAll(histories.map { RatingHistoryEntity.from(it) })
    }

    override fun findByRiotId(riotId: String, limit: Int): List<RatingHistory> =
        repo.findByRiotIdOrderByGameCreationDesc(riotId, PageRequest.of(0, limit)).map { it.toDomain() }

    /**
     * 재집계는 전체 삭제 후 같은 (riot_id, match_id) 를 다시 넣는다. 유니크 제약이 걸려 있어서
     * 삭제가 INSERT 보다 먼저 DB 에 도달해야 한다. 영속성 컨텍스트에 맡기면 flush 순서가
     * 뒤집힐 수 있으므로 즉시 실행되는 일괄 삭제를 쓴다.
     */
    @Transactional
    override fun deleteAll() = repo.deleteAllInBatch()

    override fun existsByMatchId(matchId: String): Boolean = repo.existsByMatchId(matchId)

    override fun findLatestGameCreation(): Long? = repo.findLatestGameCreation()
}
