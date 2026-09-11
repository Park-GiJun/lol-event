package com.gijun.main.application.port.out

import com.gijun.main.domain.model.rating.PlayerRating

interface PlayerRatingPort {
    fun findByRiotId(riotId: String): PlayerRating?
    fun findAllByRiotIds(riotIds: Collection<String>): List<PlayerRating>
    fun findAll(): List<PlayerRating>
    fun saveAll(ratings: List<PlayerRating>)
    fun deleteAll()
}
