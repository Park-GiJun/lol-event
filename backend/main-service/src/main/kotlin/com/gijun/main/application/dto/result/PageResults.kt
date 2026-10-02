package com.gijun.main.application.dto.result

/**
 * 홈 화면 한 장을 채우는 집계 응답.
 *
 * 홈은 대시보드 컴포넌트마다 따로 호출해서 6번 왕복했고, 그중 챔피언 티어표는
 * 156종 전체(42KB)를 받아 상위 몇 줄만 그렸다. 상위 N개는 서버가 잘라서 준다.
 */
data class HomeResult(
    val overview: OverviewStats,
    /** Elo 상위. 배치 중인 플레이어는 들어가지 않는다. */
    val topPlayers: List<EloRankEntry>,
    /** 표본을 충족한 챔피언만. 티어표 상위. */
    val topChampions: List<ChampionTierEntry>,
    val awards: WeeklyAwardsResult,
    val recentMatches: List<MatchSummaryResult>,
    val period: HomePeriod,
)

/** 이 내전 기록이 언제부터 언제까지인지. 화면 상단에 한 줄로 쓴다. */
data class HomePeriod(
    val firstMatchAt: Long?,
    val lastMatchAt: Long?,
    val totalMatches: Int,
    /** 경기에 한 번이라도 등장한 인원 */
    val playerCount: Int,
)

/**
 * 챔피언 화면 한 장을 채우는 집계 응답.
 *
 * 티어는 156종 전체에서 이 챔피언 것만 골라야 했다. 그 필터링을 서버로 옮긴다.
 */
data class ChampionPageResult(
    val detail: ChampionDetailStats,
    /** 티어표에서 이 챔피언 항목. 표본 미달이면 tier 가 "?" 다. */
    val tier: ChampionTierEntry?,
    /**
     * 라인별 라인전 지표. 상대 라이너 대비 골드·CS·딜량 격차가 들어 있다.
     * 개별 상성보다 표본이 두터워 이쪽이 실제로 읽히는 값이다.
     */
    val laneStrength: List<ChampionLaneStrength>,
    /** 같은 라인에서 맞붙은 상대별 전적. 최소 표본을 넘긴 것만. */
    val matchups: List<MatchupStat>,
    /** matchups 에 적용된 최소 표본. */
    val matchupMinGames: Int,
)

/**
 * 소환사 화면 한 장을 채우는 집계 응답.
 *
 * 이전에는 이 화면을 그리려고 6개 엔드포인트를 따로 불렀고, 그중 듀오와 라이벌은
 * 전체 조합(각 57KB, 366쌍)을 받아 화면에서 한 명 것만 골라 썼다. 필터링은 서버가 한다.
 *
 * 이 사이트는 서로 다 아는 20명 남짓이 반복해서 붙는 내전 기록이라
 * "누구와 함께 이겼고 누구에게 졌는가"가 개인 지표만큼 중요하다. teammates/opponents 가 그 축이다.
 */
data class SummonerProfileResult(
    val profile: SummonerProfile,
    val streak: SummonerStreak,
    val championStats: List<ChampionStat>,
    val positionStats: List<LaneStat>,
    val recentMatches: List<RecentMatchStat>,
    /** 같은 팀으로 함께 뛴 사람들. 함께한 경기 수 순. */
    val teammates: List<SummonerTeammate>,
    /** 상대 팀으로 만난 사람들. 맞붙은 경기 수 순. */
    val opponents: List<SummonerOpponent>,
)

data class SummonerProfile(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val adjustedWinRate: Double,
    val sampleGrade: String,
    val kda: Double,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
    /** 실력 레이팅 표시값. 리더보드에 찍히는 것과 같은 숫자다. */
    val elo: Double,
    /** 배치 중이면 null */
    val eloRank: Int?,
    /** 순위가 매겨진 전체 인원. "14위 / 41명" 처럼 쓰라고 같이 준다. */
    val eloRankedTotal: Int,
    /**
     * 이 사람의 두 레이팅 한 줄. 리더보드 항목을 그대로 싣는다 — 개인 화면과 리더보드가
     * 다른 숫자를 보여 주면 안 되므로 계산을 두 벌 두지 않는다.
     *
     * 한 경기도 반영되지 않은 사람은 null 이다.
     */
    val rating: EloRankEntry?,
)

data class SummonerStreak(
    /** 양수 = 연승, 음수 = 연패, 0 = 경기 없음 */
    val current: Int,
    /** WIN / LOSS / NONE */
    val type: String,
    val longestWin: Int,
    val longestLoss: Int,
    /** 최근 10경기 결과, 최신순. "W" / "L" */
    val recentForm: List<String>,
)

data class SummonerTeammate(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val adjustedWinRate: Double,
    val sampleGrade: String,
)

data class SummonerOpponent(
    val riotId: String,
    val games: Int,
    /** 이 소환사가 이긴 횟수 */
    val wins: Int,
    val losses: Int,
    val winRate: Int,
    val adjustedWinRate: Double,
    val sampleGrade: String,
)
