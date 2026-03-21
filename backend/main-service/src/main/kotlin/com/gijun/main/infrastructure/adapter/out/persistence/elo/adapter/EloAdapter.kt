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
                id = prev.id, riotId = domain.riotId,
                elo = domain.elo, games = domain.games, updatedAt = domain.updatedAt,
            ) else PlayerEloEntity.from(domain)
        }
        repo.saveAll(entities)
    }

    @Transactional
    override fun deleteAll() = repo.deleteAll()

    override fun findAll(): List<PlayerElo> = repo.findAll().map { it.toDomain() }
}
