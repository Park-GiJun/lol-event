package com.gijun.main.application.dto.stats.result

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
