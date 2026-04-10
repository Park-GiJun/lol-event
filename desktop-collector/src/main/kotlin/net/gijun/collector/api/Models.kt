package net.gijun.collector.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(val data: T? = null)

// ── 플레이어 통계 ────────────────────────────────

@Serializable
data class PlayerStats(
    val riotId: String = "",
    val games: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winRate: Double = 0.0,
    val avgKills: Double = 0.0,
    val avgDeaths: Double = 0.0,
    val avgAssists: Double = 0.0,
    val kda: Double = 0.0,
    val elo: Double? = null,
    val championStats: List<ChampionStat> = emptyList(),
    val recentMatches: List<RecentMatch> = emptyList(),
)

@Serializable
data class ChampionStat(
    val champion: String = "",
    val championId: Int = 0,
    val games: Int = 0,
    val wins: Int = 0,
    val winRate: Double = 0.0,
    val avgKills: Double = 0.0,
    val avgDeaths: Double = 0.0,
    val avgAssists: Double = 0.0,
    val kda: Double = 0.0,
)

@Serializable
data class RecentMatch(
    val matchId: String = "",
    val champion: String = "",
    val championId: Int = 0,
    val win: Boolean = false,
    val kills: Int = 0,
    val deaths: Int = 0,
    val assists: Int = 0,
    val damage: Int = 0,
    val cs: Int = 0,
    val gold: Int = 0,
    val gameCreation: Long = 0,
    val gameDuration: Int = 0,
    val queueId: Int = 0,
)

// ── 전체 통계 목록 ────────────────────────────────

@Serializable
data class StatsListResult(val stats: List<StatsListEntry> = emptyList())

@Serializable
data class StatsListEntry(val riotId: String = "", val games: Int = 0, val wins: Int = 0)

// ── 매치 상세 ────────────────────────────────────

@Serializable
data class MatchDetail(
    val matchId: String = "",
    val queueId: Int = 0,
    val gameCreation: Long = 0,
    val gameDuration: Int = 0,
    val participants: List<MatchParticipant> = emptyList(),
)

@Serializable
data class MatchParticipant(
    val riotId: String = "",
    val champion: String = "",
    val championId: Int = 0,
    val team: String = "",
    val win: Boolean = false,
    val kills: Int = 0,
    val deaths: Int = 0,
    val assists: Int = 0,
    val damage: Int = 0,
    val cs: Int = 0,
    val gold: Int = 0,
    val item0: Int = 0,
    val item1: Int = 0,
    val item2: Int = 0,
    val item3: Int = 0,
    val item4: Int = 0,
    val item5: Int = 0,
    val item6: Int = 0,
)

// ── 매치업 ────────────────────────────────────────

@Serializable
data class MatchupResult(
    val champion: String = "",
    val championId: Int = 0,
    val matchups: List<MatchupStat> = emptyList(),
)

@Serializable
data class MatchupStat(
    val opponent: String = "",
    val opponentId: Int = 0,
    val games: Int = 0,
    val wins: Int = 0,
    val winRate: Double = 0.0,
)
