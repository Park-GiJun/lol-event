package com.gijun.lolml.data

enum class Team { BLUE, RED }

enum class Position { TOP, JUNGLE, MID, ADC, SUPPORT, UNKNOWN }

/**
 * 스냅샷 한 경기. 필드는 로우 테이블(`matches`, `match_participants`)에 있는 것만 둔다.
 */
data class Match(
    val matchId: String,
    val queueId: Int,
    /** epoch millis. 시간순 재생의 기준이다. */
    val gameCreation: Long,
    val gameDurationSec: Int,
    val participants: List<Participant>,
) {
    val blueWin: Boolean get() = participants.first { it.team == Team.BLUE }.win
}

data class Participant(
    /** 사람을 가르는 키. Riot ID 가 바뀌어도 같은 사람이면 같아야 한다 — 추출할 때 puuid 를 넣는다. */
    val playerId: String,
    val team: Team,
    val position: Position,
    /** 픽 전 예측이라 이 경기의 피처로는 쓰지 않는다. 다음 경기부터의 상태(챔피언 폭 등)를 만드는 재료다. */
    val champion: String,
    val win: Boolean,
    // 아래는 그 경기의 결과물이다. 그 경기의 피처로 쓰지 않고, 다음 경기부터의 상태를 만드는 재료로만 쓴다.
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    /** 경기 종료 시점 누적 골드. 라인 맞대결의 승자를 가리는 재료다. */
    val gold: Int,
    /** 받아낸(감소시킨) 피해. 골드만 보면 탱커가 구조적으로 불리해서 같이 본다. */
    val damageSelfMitigated: Int,
)
