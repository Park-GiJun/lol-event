package com.gijun.main.infrastructure.adapter.out.persistence.rating

import com.gijun.main.application.port.out.persistence.PlayerRatingCommandPersistencePort
import com.gijun.main.application.port.out.persistence.PlayerRatingQueryPersistencePort
import com.gijun.main.domain.rating.model.PlayerRatingModel
import com.gijun.main.infrastructure.adapter.out.persistence.rating.PlayerRatingJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.rating.PlayerRatingJpaRepository
import com.gijun.main.shared.domain.vo.RiotId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlayerRatingQueryPersistenceAdapter(
    private val repo: PlayerRatingJpaRepository,
) : PlayerRatingQueryPersistencePort {
    override fun findByRiotId(riotId: RiotId): PlayerRatingModel? = repo.findByRiotId(riotId.value)?.toModel()

    override fun findAllByRiotIds(riotIds: Collection<String>): List<PlayerRatingModel> =
        if (riotIds.isEmpty()) emptyList() else repo.findAllByRiotIdIn(riotIds).map { it.toModel() }

    override fun findAll(): List<PlayerRatingModel> = repo.findAll().map { it.toModel() }
}

@Component
class PlayerRatingCommandPersistenceAdapter(
    private val repo: PlayerRatingJpaRepository,
) : PlayerRatingCommandPersistencePort {
    /**
     * riot_id 에 유니크가 걸려 있어서, 도메인이 들고 온 id 가 0 이어도 기존 행이 있으면
     * 그 행의 id 를 찾아 덮어써야 한다. 그러지 않으면 같은 사람을 두 번 INSERT 하려다 터진다.
     */
    @Transactional
    override fun saveAll(ratings: List<PlayerRatingModel>) {
        if (ratings.isEmpty()) return
        val existing = repo.findAllByRiotIdIn(ratings.map { it.riotId }).associate { it.riotId to it.id }
        repo.saveAll(ratings.map { PlayerRatingJpaEntity.from(it, id = existing[it.riotId] ?: it.id) })
    }

    /** 재집계는 전체 삭제 후 같은 riot_id 를 다시 넣는다. 유니크 제약 때문에 삭제가 먼저 DB 에 닿아야 한다. */
    @Transactional
    override fun deleteAll() = repo.deleteAllInBatch()
}
