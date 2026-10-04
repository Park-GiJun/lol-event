package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.rating.model.RatingHistoryModel
import com.gijun.main.shared.domain.vo.MatchId
import com.gijun.main.shared.domain.vo.RiotId

interface RatingHistoryQueryPersistencePort {
    fun findByRiotId(
        riotId: RiotId,
        limit: Int = 30,
    ): List<RatingHistoryModel>

    /** 전부. 자리 Elo 를 낼 때 쓴다 — 경기마다 열 줄이라 수천 줄 규모다. */
    fun findAll(): List<RatingHistoryModel>

    /** 이 매치가 이미 반영됐는지. Kafka 중복 배달 방어용. */
    fun existsByMatchId(matchId: MatchId): Boolean

    /** 지금까지 반영된 경기 중 가장 늦은 gameCreation. 기록이 없으면 null. */
    fun findLatestGameCreation(): Long?
}

interface RatingHistoryCommandPersistencePort {
    fun saveAll(histories: List<RatingHistoryModel>)

    fun deleteAll()
}
