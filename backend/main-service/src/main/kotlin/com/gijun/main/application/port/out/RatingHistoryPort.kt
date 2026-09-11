package com.gijun.main.application.port.out

import com.gijun.main.domain.model.rating.RatingHistory

interface RatingHistoryPort {
    fun saveAll(histories: List<RatingHistory>)
    fun findByRiotId(riotId: String, limit: Int = 30): List<RatingHistory>
    fun deleteAll()

    /** 이 매치가 이미 반영됐는지. Kafka 중복 배달 방어용. */
    fun existsByMatchId(matchId: String): Boolean

    /** 지금까지 반영된 경기 중 가장 늦은 gameCreation. 기록이 없으면 null. */
    fun findLatestGameCreation(): Long?
}
