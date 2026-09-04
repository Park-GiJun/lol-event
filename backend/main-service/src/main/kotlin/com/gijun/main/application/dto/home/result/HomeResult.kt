package com.gijun.main.application.dto.home.result

import com.gijun.main.application.dto.match.result.MatchSummaryResult
import com.gijun.main.application.dto.stats.result.ChampionTierEntry
import com.gijun.main.application.dto.stats.result.EloRankEntry
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.WeeklyAwardsResult

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
