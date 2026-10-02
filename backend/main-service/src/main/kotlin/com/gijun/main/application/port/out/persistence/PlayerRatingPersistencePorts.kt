package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.rating.model.PlayerRatingModel

interface PlayerRatingQueryPersistencePort {
    fun findByRiotId(riotId: String): PlayerRatingModel?

    fun findAllByRiotIds(riotIds: Collection<String>): List<PlayerRatingModel>

    fun findAll(): List<PlayerRatingModel>
}

interface PlayerRatingCommandPersistencePort {
    fun saveAll(ratings: List<PlayerRatingModel>)

    fun deleteAll()
}
