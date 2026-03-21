package com.gijun.main.application.port.out

import com.gijun.main.domain.model.elo.PlayerElo

interface EloPort {
    fun findByRiotId(riotId: String): PlayerElo?
    fun findAllByRiotIds(riotIds: List<String>): List<PlayerElo>
    fun saveAll(elos: List<PlayerElo>)
    fun deleteAll()
    fun findAll(): List<PlayerElo>
}
