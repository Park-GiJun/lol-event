package com.gijun.main.application.dto.stats.result

/**
 * 한 경기가 두 레이팅을 어떻게 움직였는지.
 *
 * `elo*` 는 **라인 레이팅**이다. 화면이 "Elo" 라고 부르는 값이 이제 laneElo 라서,
 * 필드 이름을 바꾸지 않고 뜻만 옮겼다. 팀 레이팅은 `team*` 로 따로 싣는다.
 */
data class EloHistoryEntry(
    val matchId: String,
    val eloBefore: Double,
    val eloAfter: Double,
    val delta: Double,
    val win: Boolean,
    /** 라인 맞대결 결과. WIN / LOSS / NONE(대결 불성립 — 칼바람이거나 포지션이 깨진 경기). */
    val laneResult: String,
    /** 라인 상대. 대결이 없었으면 null. */
    val laneOpponent: String?,
    val teamEloBefore: Double,
    val teamEloAfter: Double,
    val teamDelta: Double,
    val gameCreation: Long,
)

data class PlayerEloHistoryResult(
    val riotId: String,
    /** 라인 레이팅 원값. */
    val currentElo: Double,
    val eloRank: Int?,
    val history: List<EloHistoryEntry>,
)
