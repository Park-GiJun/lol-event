package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.rating.model.PlayerRatingModel
import com.gijun.main.shared.domain.vo.RiotId

interface PlayerRatingQueryPersistencePort {
    fun findByRiotId(riotId: RiotId): PlayerRatingModel?

    fun findAllByRiotIds(riotIds: Collection<String>): List<PlayerRatingModel>

    fun findAll(): List<PlayerRatingModel>
}

interface PlayerRatingCommandPersistencePort {
    fun saveAll(ratings: List<PlayerRatingModel>)

    fun deleteAll()
}
