package com.gijun.main.application.port.out

import com.gijun.main.domain.model.elo.PlayerEloHistory

interface EloHistoryPort {
    fun saveAll(histories: List<PlayerEloHistory>)
    fun findByRiotId(riotId: String, limit: Int = 30): List<PlayerEloHistory>
    fun deleteAll()
}
