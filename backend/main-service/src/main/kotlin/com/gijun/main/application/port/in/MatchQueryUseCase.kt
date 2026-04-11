package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.match.result.MatchResult

interface GetMatchesUseCase {
    fun getAll(mode: String): List<MatchResult>
    fun getById(matchId: String): MatchResult?
}
