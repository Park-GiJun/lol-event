package com.gijun.main.application.dto.stats.result

/**
 * 하루치 내전. 세션은 **오전 6시에 시작하는 하루**다 —
 * 경계와 이유는 [com.gijun.main.domain.service.SessionClock] 에 있다.
 *
 * ## 단위 규약
 * - 시간은 ms, 길이는 분.
 * - 비율은 0~100, 평균은 소수 첫째 자리.
 * - `mode = "all"` 은 **칼바람을 포함하지 않는다**(`normal` 과 같다). 기존 함정을 그대로 물려받는다.
 *
 * 타임라인 수치의 모집단은 전체 경기가 아니라 [timelineGames] 다. 새 수집기로 받은 경기에만
 * 타임라인이 있고 과거 백필은 불가능하다.
 */
data class SessionDetailResult(
    /** `yyyy-MM-dd`. */
    val date: String,
    val games: Int,
    /** 그중 타임라인이 있는 경기 수. 아래 타임라인 수치 전부의 분모다. */
    val timelineGames: Int,
    val firstGameAt: Long,
    val lastGameAt: Long,
    val totalDurationMin: Int,
    val totalKills: Int,
    val pentaKills: Int,
    val team100Wins: Int,
    val team200Wins: Int,
    val matches: List<SessionMatchEntry>,
    val players: List<SessionPlayerEntry>,
    /** 세션 전체에서 감지된 교전 수(킬 3개 이상). */
    val teamFights: Int,
    /** 팀별 한타 승수. 동수로 끝난 교전은 어느 쪽에도 안 들어간다. */
    val team100FightWins: Int,
    val team200FightWins: Int,
    /** 최대 연승 기록. 팀이 매 판 바뀌므로 **사람** 기준이다. */
    val longestWinStreak: SessionStreak?,
    val longestLossStreak: SessionStreak?,
    /** 15분에 가장 크게 뒤지고도 이긴 경기. 없으면 null. */
    val biggestComeback: SessionMatchEntry?,
    val longestGame: SessionMatchEntry?,
    val shortestGame: SessionMatchEntry?,
)

data class SessionMatchEntry(
    val matchId: String,
    val gameCreation: Long,
    val durationSec: Int,
    val winnerTeamId: Int?,
    val totalKills: Int,
    val hasTimeline: Boolean,
    /** 스파크라인용. 블루 − 레드, index = 분. 타임라인이 없으면 빈 목록. */
    val teamGoldDiffByMinute: List<Int>,
    /** 15분 팀 골드 격차(블루 − 레드). 타임라인이 없으면 null. */
    val goldDiffAt15: Int?,
)

data class SessionPlayerEntry(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val kda: Double,
    /** 타임라인이 있는 경기만. 없으면 null. */
    val avgGoldDiff15: Double?,
    val laneGames: Int,
    val earlyKills: Int,
    val earlyDeaths: Int,
    val soloKills: Int,
)

data class SessionStreak(
    val riotId: String,
    val length: Int,
)
