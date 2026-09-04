package com.gijun.main.domain.model.elo

import java.time.LocalDateTime

data class PlayerEloHistory(
    val id: Long = 0,
    val riotId: String,
    val matchId: String,
    val eloBefore: Double,
    val eloAfter: Double,
    val delta: Double,
    val win: Boolean,
    /**
     * 같은 포지션 상대와 비교한 라인전 점수(0~1, 0.5가 호각).
     * 같은 승리인데 왜 변동폭이 다른지 설명하는 근거라 함께 기록한다.
     */
    val lanePerformance: Double = 0.5,
    val gameCreation: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
