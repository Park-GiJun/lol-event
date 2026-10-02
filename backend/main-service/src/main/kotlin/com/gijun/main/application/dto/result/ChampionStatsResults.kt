package com.gijun.main.application.dto.result

data class ChampionPlayerStat(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
)

data class ChampionItemStat(
    val itemId: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
)

/**
 * 챔피언별 룬 조합 성적.
 *
 * 아이템은 칸 단위라 한 경기에서 여섯 번 세지만, 룬은 조합이 경기당 하나라
 * picks 가 곧 그 조합을 쓴 경기 수다.
 */
data class ChampionRuneStat(
    /** 핵심 룬(키스톤) id. */
    val keystone: Int,
    /** 주 계열 id (정밀 8000 / 지배 8100 / 마법 8200 / 영감 8300 / 결의 8400). */
    val primaryStyle: Int,
    /** 보조 계열 id. */
    val subStyle: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
)

data class ChampionLaneStat(
    val position: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
)

data class ChampionDetailStats(
    val champion: String,
    val championId: Int,
    val totalGames: Int,
    val totalWins: Int,
    val winRate: Int,
    val players: List<ChampionPlayerStat>,
    val itemStats: List<ChampionItemStat> = emptyList(),
    /** 룬 정보가 실려 오지 않은 경기만 있으면 빈 목록이다. */
    val runeStats: List<ChampionRuneStat> = emptyList(),
    val laneStats: List<ChampionLaneStat> = emptyList(),
)

/**
 * 같은 라인 상대와의 평균 격차. 양수면 이 챔피언이 앞선다.
 *
 * 승률만으로는 "왜" 를 못 본다. 이겼는지가 아니라 라인에서 무엇을 얼마나 벌었는지가
 * 챔피언 상성의 실제 내용이다.
 */
data class LaneGap(
    val goldDiff: Int,
    val csDiff: Double,
    val damageDiff: Int,
    val killDiff: Double,
    val visionDiff: Double,
)

/**
 * 챔피언 단위 라인전 지표.
 *
 * 개별 상성(A vs B)은 154경기에서 574조합이 나오고 중앙 표본이 1회라 그리드로 못 만든다.
 * 반면 "이 챔피언이 라인전에서 평균 얼마나 앞서나" 는 (챔피언 x 라인) 203조합에
 * 중앙 2회, 5회 이상이 49개라 읽을 만하다. 그래서 축을 하나 위로 올렸다.
 */
data class ChampionLaneStrength(
    val champion: String,
    val championId: Int,
    val position: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    /** 표본을 50% 쪽으로 당긴 승률. 정렬은 이 값으로 한다. */
    val adjustedWinRate: Double,
    val sampleGrade: String,
    val gap: LaneGap,
)

/** 개별 상성. 표본이 얇아 최소 경기 수를 넘긴 것만 내보낸다. */
data class MatchupStat(
    val opponent: String,
    val opponentId: Int,
    val position: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val adjustedWinRate: Double,
    val sampleGrade: String,
    val gap: LaneGap,
)

data class ChampionMatchupResult(
    val champion: String,
    val championId: Int,
    /** 라인별 라인전 지표. 한 챔피언이 여러 라인을 가기도 한다. */
    val laneStrength: List<ChampionLaneStrength>,
    /** 유리한 순으로 정렬된 개별 상성. */
    val matchups: List<MatchupStat>,
    /** 개별 상성에 적용한 최소 표본. 화면에서 "N경기 이상만" 이라고 밝히기 위한 값. */
    val minGames: Int,
)

data class ChampionCertEntry(
    val riotId: String,
    val champion: String,
    val championId: Int,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Double,
    /** 표본을 50% 쪽으로 당긴 승률. 정렬은 반드시 이 값으로 한다. */
    val adjustedWinRate: Double,
    /** HIGH / MEDIUM / LOW / INSUFFICIENT */
    val sampleGrade: String,
    val certified: Boolean,
)

data class ChampionCertificateResult(
    val certifiedMasters: List<ChampionCertEntry>,
    val topChampionMasters: Map<String, ChampionCertEntry>,
)

data class ChampionTierEntry(
    val champion: String,
    val championId: Int,
    val tier: String,
    val tierScore: Double,
    val games: Int,
    /** 관측 승률. 표본이 작으면 그대로 믿으면 안 된다. 항상 games 와 같이 보여줄 것. */
    val winRate: Int,
    /** 전체 평균 쪽으로 끌어당긴 승률. 정렬은 이 값으로 한다. */
    val adjustedWinRate: Double,
    /** HIGH / MEDIUM / LOW / INSUFFICIENT */
    val sampleGrade: String,
    val kda: Double,
    val pickRate: Double,
    val avgDamage: Double,
)

data class ChampionTierResult(
    val tierList: List<ChampionTierEntry>,
    val byTier: Map<String, List<ChampionTierEntry>>,
    val totalMatches: Int,
)

data class BanEntry(
    val champion: String,
    val championId: Int,
    val banCount: Int,
    val banRate: Double, // banCount / totalGames
)

data class BanAnalysisResult(
    val topBanned: List<BanEntry>,
    val totalGamesAnalyzed: Int,
    val mostBannedChampion: String?,
)
