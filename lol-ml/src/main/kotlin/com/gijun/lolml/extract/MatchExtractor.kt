package com.gijun.lolml.extract

import com.gijun.lolml.data.Match

/**
 * `lol_event.matches` ⨝ `lol_event.match_participants` 를 시간순으로 읽는다.
 * 가공 테이블(`player_elo`, `*_stats_snapshot`)은 읽지 않는다.
 */
class MatchExtractor(
    private val config: DbConfig,
) {
    /**
     * 정렬은 `ORDER BY game_creation, match_id` 로 SQL 에서 동점까지 고정한다.
     * PostgreSQL 은 `autoCommit = false` + `fetchSize` 여야 스트리밍된다.
     * 참가자 행 10개를 경기 하나로 묶어 [onMatch] 에 넘긴다.
     */
    fun extract(onMatch: (Match) -> Unit): Unit = TODO()
}
