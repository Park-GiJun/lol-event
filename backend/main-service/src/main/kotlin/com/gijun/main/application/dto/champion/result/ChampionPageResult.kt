package com.gijun.main.application.dto.champion.result

import com.gijun.main.application.dto.stats.result.ChampionDetailStats
import com.gijun.main.application.dto.stats.result.ChampionTierEntry
import com.gijun.main.application.dto.stats.result.MatchupStat

/**
 * 챔피언 화면 한 장을 채우는 집계 응답.
 *
 * 시너지는 전체 조합에서, 티어는 156종 전체에서 이 챔피언 것만 골라야 했다.
 * 그 필터링을 서버로 옮긴다.
 */
data class ChampionPageResult(
    val detail: ChampionDetailStats,
    /** 티어표에서 이 챔피언 항목. 표본 미달이면 tier 가 "?" 다. */
    val tier: ChampionTierEntry?,
    /** 이 챔피언을 상대로 만났을 때의 전적 */
    val matchups: List<MatchupStat>,
    /** 이 챔피언과 같은 팀이었을 때 잘 맞은 조합 */
)
