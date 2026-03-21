package com.gijun.main.infrastructure.adapter.out.persistence.elo.adapter

import com.gijun.main.application.port.out.EloHistoryPort
import com.gijun.main.domain.model.elo.PlayerEloHistory
import com.gijun.main.infrastructure.adapter.out.persistence.elo.entity.PlayerEloHistoryEntity
import com.gijun.main.infrastructure.adapter.out.persistence.elo.repository.PlayerEloHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EloHistoryAdapter(private val repo: PlayerEloHistoryRepository) : EloHistoryPort {

    @Transactional
    override fun saveAll(histories: List<PlayerEloHistory>) {
        repo.saveAll(histories.map { PlayerEloHistoryEntity.from(it) })
    }

    override fun findByRiotId(riotId: String, limit: Int): List<PlayerEloHistory> =
        repo.findByRiotIdOrderByGameCreationDesc(riotId, PageRequest.of(0, limit))
            .map { it.toDomain() }

    @Transactional
    override fun deleteAll() = repo.deleteAll()
}
