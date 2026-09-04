package com.gijun.main.application.dto.champion.result

import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionLaneStrength
import com.gijun.main.application.dto.stats.result.ChampionTierEntry
import com.gijun.main.application.dto.stats.result.MatchupStat

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
