package com.gijun.main.domain.model.rating

import java.time.LocalDateTime

/**
 * 경기 한 판이 한 사람의 두 레이팅을 어떻게 움직였는지. 화면에서 "왜 이만큼 올랐나"를
 * 설명하는 근거이자, 재집계가 제대로 돌았는지 되짚는 감사 로그다.
 *
 * 라인 대결이 성립하지 않은 경기(포지션이 깨졌거나, 같은 포지션에 두 명이 잡혔거나, 칼바람)는
 * [laneDelta] 가 0 이고 [laneResult] 가 [LaneResult.NONE] 이다. 그런 경기에도 팀 레이팅은 움직인다.
 */
data class RatingHistory(
    val id: Long = 0,
    val riotId: String,
    val matchId: String,
    val laneBefore: Double,
    val laneAfter: Double,
    val laneResult: LaneResult,
    /** 라인 상대. 대결이 없었으면 null. */
    val laneOpponent: String? = null,
    val teamBefore: Double,
    val teamAfter: Double,
    val win: Boolean,
    val gameCreation: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    val laneDelta: Double get() = laneAfter - laneBefore
    val teamDelta: Double get() = teamAfter - teamBefore
}

enum class LaneResult { WIN, LOSS, NONE }
