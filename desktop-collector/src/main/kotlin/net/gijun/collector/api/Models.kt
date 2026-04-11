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

// ── 듀오 시너지 ──────────────────────────────────

@Serializable
data class DuoSynergyResult(val duos: List<DuoSynergy> = emptyList())

@Serializable
data class DuoSynergy(
    val player1: String = "",
    val player2: String = "",
    val games: Int = 0,
    val wins: Int = 0,
    val winRate: Double = 0.0,
    val combinedKda: Double = 0.0,
)

// ── 라이벌 매치업 ─────────────────────────────────

@Serializable
data class RivalMatchupResult(val rivals: List<RivalEntry> = emptyList())

@Serializable
data class RivalEntry(
    val player1: String = "",
    val player2: String = "",
    val games: Int = 0,
    val player1Wins: Int = 0,
    val player2Wins: Int = 0,
)

// ── 챔피언 티어 ──────────────────────────────────

@Serializable
data class ChampionTierResult(val tiers: List<ChampionTierEntry> = emptyList())

@Serializable
data class ChampionTierEntry(
    val champion: String = "",
    val championId: Int = 0,
    val tier: String = "",
    val games: Int = 0,
    val winRate: Double = 0.0,
    val kda: Double = 0.0,
    val picks: Int = 0,
)

// ── 밴 분석 ──────────────────────────────────────

@Serializable
data class BanAnalysisResult(val bans: List<BanEntry> = emptyList())

@Serializable
data class BanEntry(
    val champion: String = "",
    val championId: Int = 0,
    val banCount: Int = 0,
    val banRate: Double = 0.0,
    val winRateWhenNotBanned: Double = 0.0,
)

// ── Elo 리더보드 ─────────────────────────────────

@Serializable
data class EloLeaderboardResult(val players: List<EloEntry> = emptyList())

@Serializable
data class EloEntry(
    val riotId: String = "",
    val elo: Double = 0.0,
    val games: Int = 0,
    val wins: Int = 0,
    val tier: String = "",
)

// ── Elo 히스토리 ─────────────────────────────────

@Serializable
data class EloHistoryResult(val history: List<EloHistoryEntry> = emptyList())

@Serializable
data class EloHistoryEntry(
    val matchId: String = "",
    val elo: Double = 0.0,
    val delta: Double = 0.0,
    val champion: String = "",
    val win: Boolean = false,
    val gameCreation: Long = 0,
)

// ── 스트릭 ───────────────────────────────────────

@Serializable
data class PlayerStreakResult(
    val currentStreak: Int = 0,
    val currentStreakType: String = "",
    val longestWinStreak: Int = 0,
    val longestLossStreak: Int = 0,
    val recentForm: List<Boolean> = emptyList(),
)

// ── 어워즈 ───────────────────────────────────────

@Serializable
data class AwardsResult(
    val pentaKillHero: AwardEntry? = null,
    val highestWinRate: AwardEntry? = null,
    val mostDeaths: AwardEntry? = null,
    val kdaKing: AwardEntry? = null,
)

@Serializable
data class AwardEntry(
    val riotId: String = "",
    val displayValue: String = "",
    val games: Int = 0,
)

// ── 멀티킬 하이라이트 ─────────────────────────────

@Serializable
data class MultikillHighlightsResult(
    val pentaKillEvents: List<MultikillEvent> = emptyList(),
    val quadraKillEvents: List<MultikillEvent> = emptyList(),
)

@Serializable
data class MultikillEvent(
    val riotId: String = "",
    val champion: String = "",
    val gameCreation: Long = 0,
    val matchId: String = "",
)

// ── MVP 랭킹 ─────────────────────────────────────

@Serializable
data class MvpRankingResult(val rankings: List<MvpEntry> = emptyList())

@Serializable
data class MvpEntry(
    val riotId: String = "",
    val mvpScore: Double = 0.0,
    val games: Int = 0,
    val mvpCount: Int = 0,
)

// ── 포지션별 챔피언 풀 ────────────────────────────

@Serializable
data class PositionPoolResult(val players: List<PositionPoolEntry> = emptyList())

@Serializable
data class PositionPoolEntry(
    val riotId: String = "",
    val positions: Map<String, Int> = emptyMap(),
    val mainPosition: String = "",
)

// ── 플레이스타일 DNA ──────────────────────────────

@Serializable
data class PlaystyleDnaResult(val players: List<DnaEntry> = emptyList())

@Serializable
data class DnaEntry(
    val riotId: String = "",
    val aggression: Double = 0.0,
    val farming: Double = 0.0,
    val vision: Double = 0.0,
    val teamfight: Double = 0.0,
    val objective: Double = 0.0,
    val archetype: String = "",
)

// ── 개요 ─────────────────────────────────────────

@Serializable
data class OverviewResult(
    val matchCount: Int = 0,
    val avgGameMinutes: Double = 0.0,
    val totalDragonKills: Int = 0,
    val totalBaronKills: Int = 0,
    val totalTowerKills: Int = 0,
    val totalCs: Int = 0,
)

// ── 비교 ─────────────────────────────────────────

@Serializable
data class CompareResult(
    val player1: PlayerStats? = null,
    val player2: PlayerStats? = null,
)

// ── Riot API 프로필 ──────────────────────────────

@Serializable
data class RiotProfile(
    val riotId: String = "",
    val puuid: String? = null,
    val summonerLevel: Long? = null,
    val profileIconId: Int? = null,
    val soloRank: RankedInfo? = null,
    val flexRank: RankedInfo? = null,
    val topMastery: List<MasteryInfo> = emptyList(),
)

@Serializable
data class RankedInfo(
    val tier: String = "",
    val rank: String = "",
    val lp: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winRate: Double = 0.0,
)

@Serializable
data class MasteryInfo(
    val championId: Int = 0,
    val level: Int = 0,
    val points: Int = 0,
)
