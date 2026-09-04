package com.gijun.main.infrastructure.adapter.out.persistence.elo.adapter

import com.gijun.main.application.port.out.EloPort
import com.gijun.main.domain.model.elo.PlayerElo
import com.gijun.main.infrastructure.adapter.out.persistence.elo.entity.PlayerEloEntity
import com.gijun.main.infrastructure.adapter.out.persistence.elo.repository.PlayerEloRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EloAdapter(private val repo: PlayerEloRepository) : EloPort {

    override fun findByRiotId(riotId: String): PlayerElo? =
        repo.findByRiotId(riotId)?.toDomain()

    override fun findAllByRiotIds(riotIds: List<String>): List<PlayerElo> =
        repo.findAllByRiotIdIn(riotIds).map { it.toDomain() }

    @Transactional
    override fun saveAll(elos: List<PlayerElo>) {
        val existing = repo.findAllByRiotIdIn(elos.map { it.riotId }).associateBy { it.riotId }
        val entities = elos.map { domain ->
            val prev = existing[domain.riotId]
            if (prev != null) PlayerEloEntity(
                id          = prev.id,
                riotId      = domain.riotId,
                elo         = domain.elo,
                games       = domain.games,
                wins        = domain.wins,
                losses      = domain.losses,
                winStreak   = domain.winStreak,
                lossStreak  = domain.lossStreak,
                updatedAt   = domain.updatedAt,
            ) else PlayerEloEntity.from(domain)
        }
        repo.saveAll(entities)
    }

    /** 재집계는 전체 삭제 후 같은 riot_id 를 다시 넣는다. 유니크 제약 때문에 삭제가 먼저 DB 에 닿아야 한다. */
    @Transactional
    override fun deleteAll() = repo.deleteAllInBatch()

    override fun findAll(): List<PlayerElo> = repo.findAll().map { it.toDomain() }
}
