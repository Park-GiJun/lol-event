package com.gijun.main.application.dto.result

/**
 * 오브젝트 하나를 챙긴 팀의 승률.
 *
 * 챙기는 방식이 두 가지다. 퍼블·첫 드래곤처럼 "먼저" 잡은 팀이 기록되는 것과,
 * 공허 유충처럼 먼저 표시가 없어 "더 많이" 먹은 팀으로 봐야 하는 것이다.
 * 화면에서 문구를 다르게 써야 해서 [basis] 로 구분한다.
 */
data class ObjectiveStat(
    val objective: String,
    val label: String,
    /** FIRST = 먼저 챙긴 팀 / MAJORITY = 더 많이 챙긴 팀. */
    val basis: String,
    val totalGames: Int,
    /** 그 오브젝트의 주인이 가려진 경기 수. MAJORITY 는 양 팀이 같으면 안 센다. */
    val gamesWithFirst: Int,
    val winsWithFirst: Int,
    val winRateWithFirst: Int,
    val winRateWithout: Int,
)

data class ObjectiveCorrelationResult(
    val totalGames: Int,
    val objectives: List<ObjectiveStat>,
)

/**
 * 요일·시간대별 경기 수.
 *
 * 예전에는 winRate 도 함께 내보냈는데 양쪽 다 값이 성립하지 않았다.
 * 요일 쪽은 "이긴 팀이 하나라도 있으면 1승"으로 세고 있어서 늘 100% 근처(87~100%)였고,
 * 시간 쪽은 계산 자체를 안 하고 0.0 을 박아 내보내고 있었다.
 *
 * 애초에 양 팀이 모두 우리 쪽인 내전에서는 경기 단위 승률이라는 게 정의되지 않는다.
 * 화면도 games 만 그리고 있었으므로 필드를 걷어냈다.
 */
data class DayPatternEntry(
    val dayOfWeek: Int, // 1=Monday..7=Sunday (ISO)
    val dayName: String, // "월", "화", ...
    val sessions: Int,
    val games: Int,
)

data class HourPatternEntry(
    val hour: Int,
    val games: Int,
)

data class TimePatternResult(
    val byDay: List<DayPatternEntry>,
    val byHour: List<HourPatternEntry>,
    val busiestDay: String?,
    val busiestHour: Int?,
    val totalGames: Int,
)

data class GameLengthBucket(
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgDamage: Double,
    val avgCsPerMin: Double,
)

data class GameLengthTendencyEntry(
    val riotId: String,
    val totalGames: Int,
    val shortGame: GameLengthBucket,
    val midGame: GameLengthBucket,
    val longGame: GameLengthBucket,
    val tendency: String,
)

data class ChampionLengthTendency(
    val champion: String,
    val championId: Int,
    val shortWinRate: Int,
    val midWinRate: Int,
    val longWinRate: Int,
    val bestLength: String,
)

data class GameLengthTendencyResult(
    val players: List<GameLengthTendencyEntry>,
    val championTendencies: List<ChampionLengthTendency>,
)

data class EarlyGameDominanceEntry(
    val riotId: String,
    val games: Int,
    val firstBloodRate: Double,
    val firstTowerRate: Double,
    val earlyGameScore: Double,
    val firstBloodWinRate: Int,
    val noFirstBloodWinRate: Int,
    val badges: List<String>,
)

data class EarlyGameDominanceResult(
    val rankings: List<EarlyGameDominanceEntry>,
    val firstBloodKing: String?,
    val towerDestroyer: String?,
    val overallFirstBloodWinRate: Double,
    val overallFirstTowerWinRate: Double,
)

/**
 * 역전 지표.
 *
 * surrenderGames / surrenderWinRate 는 걷어냈다. 조기 항복 경기를 세던 값인데
 * game_ended_in_early_surrender 가 전 행 false 라 항상 0 이었다.
 */
data class ComebackIndexEntry(
    val riotId: String,
    val totalGames: Int,
    val totalWinRate: Int,
    val contestGames: Int,
    val contestWinRate: Int,
    val comebackBonus: Int,
    val isKing: Boolean,
)

data class ComebackMatchEntry(
    val matchId: String,
    val gameCreation: Long,
    val gameDurationMin: Double,
    val winnerParticipants: List<String>,
)

data class ComebackIndexResult(
    val rankings: List<ComebackIndexEntry>,
    val comebackKing: String?,
    val topComebackMatches: List<ComebackMatchEntry>,
)
