package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.SessionDetailResult
import com.gijun.main.application.dto.result.SessionEntry
import com.gijun.main.application.dto.result.SessionMatchEntry
import com.gijun.main.application.dto.result.SessionPlayerEntry
import com.gijun.main.application.dto.result.SessionReportResult
import com.gijun.main.application.dto.result.SessionStreak
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "SessionEntry")
data class SessionEntryResponse(
    @field:Schema(description = "`yyyy-MM-dd`. 세션 상세(`/api/stats/sessions/{date}`)의 식별자다.")
    val date: String,
    @field:Schema(description = "그 세션의 경기들. 시작 시각 오름차순. 상세로 들어가려면 이게 필요하다.")
    val matchIds: List<String>,
    val games: Int,
    val totalDurationMin: Int,
    val sessionMvp: String?,
    val sessionMvpKda: Double,
    val team100Wins: Int,
    val team200Wins: Int,
    val totalKills: Int,
    val pentaKills: Int,
    val participants: List<String>,
) {
    companion object {
        fun from(result: SessionEntry) =
            SessionEntryResponse(
                date = result.date,
                matchIds = result.matchIds,
                games = result.games,
                totalDurationMin = result.totalDurationMin,
                sessionMvp = result.sessionMvp,
                sessionMvpKda = result.sessionMvpKda,
                team100Wins = result.team100Wins,
                team200Wins = result.team200Wins,
                totalKills = result.totalKills,
                pentaKills = result.pentaKills,
                participants = result.participants,
            )
    }
}

@Schema(name = "SessionReportResult")
data class SessionReportResponse(
    val sessions: List<SessionEntryResponse>,
    val totalSessions: Int,
) {
    companion object {
        fun from(result: SessionReportResult) =
            SessionReportResponse(
                sessions = result.sessions.map(SessionEntryResponse::from),
                totalSessions = result.totalSessions,
            )
    }
}

@Schema(name = "SessionDetailResult")
data class SessionDetailResponse(
    @field:Schema(description = "`yyyy-MM-dd`.")
    val date: String,
    val games: Int,
    @field:Schema(description = "그중 타임라인이 있는 경기 수. 아래 타임라인 수치 전부의 분모다.")
    val timelineGames: Int,
    val firstGameAt: Long,
    val lastGameAt: Long,
    val totalDurationMin: Int,
    val totalKills: Int,
    val pentaKills: Int,
    val team100Wins: Int,
    val team200Wins: Int,
    val matches: List<SessionMatchEntryResponse>,
    val players: List<SessionPlayerEntryResponse>,
    @field:Schema(description = "세션 전체에서 감지된 교전 수(킬 3개 이상).")
    val teamFights: Int,
    @field:Schema(description = "팀별 한타 승수. 동수로 끝난 교전은 어느 쪽에도 안 들어간다.")
    val team100FightWins: Int,
    val team200FightWins: Int,
    @field:Schema(description = "최대 연승 기록. 팀이 매 판 바뀌므로 **사람** 기준이다.")
    val longestWinStreak: SessionStreakResponse?,
    val longestLossStreak: SessionStreakResponse?,
    @field:Schema(description = "15분에 가장 크게 뒤지고도 이긴 경기. 없으면 null.")
    val biggestComeback: SessionMatchEntryResponse?,
    val longestGame: SessionMatchEntryResponse?,
    val shortestGame: SessionMatchEntryResponse?,
) {
    companion object {
        fun from(result: SessionDetailResult) =
            SessionDetailResponse(
                date = result.date,
                games = result.games,
                timelineGames = result.timelineGames,
                firstGameAt = result.firstGameAt,
                lastGameAt = result.lastGameAt,
                totalDurationMin = result.totalDurationMin,
                totalKills = result.totalKills,
                pentaKills = result.pentaKills,
                team100Wins = result.team100Wins,
                team200Wins = result.team200Wins,
                matches = result.matches.map(SessionMatchEntryResponse::from),
                players = result.players.map(SessionPlayerEntryResponse::from),
                teamFights = result.teamFights,
                team100FightWins = result.team100FightWins,
                team200FightWins = result.team200FightWins,
                longestWinStreak = result.longestWinStreak?.let(SessionStreakResponse::from),
                longestLossStreak = result.longestLossStreak?.let(SessionStreakResponse::from),
                biggestComeback = result.biggestComeback?.let(SessionMatchEntryResponse::from),
                longestGame = result.longestGame?.let(SessionMatchEntryResponse::from),
                shortestGame = result.shortestGame?.let(SessionMatchEntryResponse::from),
            )
    }
}

@Schema(name = "SessionMatchEntry")
data class SessionMatchEntryResponse(
    val matchId: String,
    val gameCreation: Long,
    val durationSec: Int,
    val winnerTeamId: Int?,
    val totalKills: Int,
    val hasTimeline: Boolean,
    @field:Schema(description = "스파크라인용. 블루 − 레드, index = 분. 타임라인이 없으면 빈 목록.")
    val teamGoldDiffByMinute: List<Int>,
    @field:Schema(description = "15분 팀 골드 격차(블루 − 레드). 타임라인이 없으면 null.")
    val goldDiffAt15: Int?,
) {
    companion object {
        fun from(result: SessionMatchEntry) =
            SessionMatchEntryResponse(
                matchId = result.matchId,
                gameCreation = result.gameCreation,
                durationSec = result.durationSec,
                winnerTeamId = result.winnerTeamId,
                totalKills = result.totalKills,
                hasTimeline = result.hasTimeline,
                teamGoldDiffByMinute = result.teamGoldDiffByMinute,
                goldDiffAt15 = result.goldDiffAt15,
            )
    }
}

@Schema(name = "SessionPlayerEntry")
data class SessionPlayerEntryResponse(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val kda: Double,
    @field:Schema(description = "타임라인이 있는 경기만. 없으면 null.")
    val avgGoldDiff15: Double?,
    val laneGames: Int,
    val earlyKills: Int,
    val earlyDeaths: Int,
    val soloKills: Int,
) {
    companion object {
        fun from(result: SessionPlayerEntry) =
            SessionPlayerEntryResponse(
                riotId = result.riotId,
                games = result.games,
                wins = result.wins,
                kills = result.kills,
                deaths = result.deaths,
                assists = result.assists,
                kda = result.kda,
                avgGoldDiff15 = result.avgGoldDiff15,
                laneGames = result.laneGames,
                earlyKills = result.earlyKills,
                earlyDeaths = result.earlyDeaths,
                soloKills = result.soloKills,
            )
    }
}

@Schema(name = "SessionStreak")
data class SessionStreakResponse(
    val riotId: String,
    val length: Int,
) {
    companion object {
        fun from(result: SessionStreak) =
            SessionStreakResponse(
                riotId = result.riotId,
                length = result.length,
            )
    }
}
