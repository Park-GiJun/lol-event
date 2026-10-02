package com.gijun.main.application.dto.result

data class MultiKillEvent(
    val riotId: String,
    val champion: String,
    val championId: Int,
    val multiKillType: String, // "PENTA", "QUADRA", "TRIPLE", "DOUBLE"
    val matchId: String,
    val gameCreation: Long,
)

data class PlayerMultiKillStat(
    val riotId: String,
    val pentaKills: Int,
    val quadraKills: Int,
    val tripleKills: Int,
    val doubleKills: Int,
    val topChampion: String?,
    val topChampionId: Int?,
)

data class MultiKillHighlightsResult(
    val pentaKillEvents: List<MultiKillEvent>, // 전체 펜타킬 이벤트 (최신순)
    val recentHighlights: List<MultiKillEvent>, // 최근 쿼드라 이상 이벤트 20개
    val playerRankings: List<PlayerMultiKillStat>, // 플레이어별 멀티킬 합계 (펜타킬 내림차순)
)

data class ChaosMatchEntry(
    val matchId: String,
    val gameCreation: Long,
    val gameDurationMin: Double,
    val chaosIndex: Double,
    val totalKills: Int,
    val killDensity: Double,
    val multiKillScore: Int,
    val gameTypeTag: String, // "혈전", "학살", "운영 접전", "일반"
    val participants: List<String>, // riotId 목록
)

data class ChaosMatchResult(
    val topChaosMatches: List<ChaosMatchEntry>, // 혼돈 지수 상위 10
    val topBloodBathMatches: List<ChaosMatchEntry>, // 학살 태그 상위 5
    val topStrategicMatches: List<ChaosMatchEntry>, // 운영 접전 상위 5
    val avgChaosIndex: Double,
)

data class DamagePlayerEntry(
    val riotId: String,
    val games: Int,
    val avgPhysical: Int,
    val avgMagic: Int,
    val avgTrue: Int,
    val avgTotal: Int,
    val avgMitigated: Int,
    val avgTurretDmg: Int,
    val physicalRate: Double,
    val magicRate: Double,
    val trueRate: Double,
    val damageProfile: String,
)

data class DamageAnalysisResult(
    val players: List<DamagePlayerEntry>,
)

/**
 * 항복 분석.
 *
 * 조기 항복(15분 이전 만장일치 항복) 관련 필드를 전부 걷어냈다.
 * game_ended_in_early_surrender / caused_early_surrender / early_surrender_accomplice /
 * team_early_surrendered 가 수집분 1,716행 전부 false 라 화면에 0 만 깔려 있었다.
 * 일반 항복(game_ended_in_surrender)은 158행에 살아 있어 그대로 둔다.
 */
data class SurrenderPlayerEntry(
    val riotId: String,
    val games: Int,
    val surrenderGames: Int,
    val surrenderRate: Double,
)

data class SurrenderAnalysisResult(
    val totalGames: Int,
    val surrenderGames: Int,
    val overallSurrenderRate: Double,
    val players: List<SurrenderPlayerEntry>,
)

/**
 * 후반 활약 지표.
 *
 * firstInhibitorRate 는 걷어냈다. first_inhibitor_kill / first_inhibitor_assist 가
 * 수집분 1,716행 전부 false 라 전원 0.0 으로 나란히 서던 칸이다.
 */
data class LateGamePlayerEntry(
    val riotId: String,
    val games: Int,
    val avgInhibitorKills: Double,
    val avgChampLevel: Double,
    val avgLongestTimeSpentLiving: Int,
    val avgLargestKillingSpree: Double,
    val avgLargestMultiKill: Double,
    val lateGameScore: Double,
)

data class LateGameResult(
    val players: List<LateGamePlayerEntry>,
)
