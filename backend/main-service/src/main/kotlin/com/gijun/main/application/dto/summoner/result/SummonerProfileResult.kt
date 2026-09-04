package com.gijun.main.application.dto.summoner.result

import com.gijun.main.application.dto.stats.result.ChampionStat
import com.gijun.main.application.dto.stats.result.LaneStat
import com.gijun.main.application.dto.stats.result.RecentMatchStat

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
    val elo: Double,
    /** 배치 중이면 null */
    val eloRank: Int?,
    /** 순위가 매겨진 전체 인원. "14위 / 41명" 처럼 쓰라고 같이 준다. */
    val eloRankedTotal: Int,
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
