package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.match.result.MatchResult
import com.gijun.main.application.port.`in`.GetMatchesUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMatchesHandler(private val matchPersistencePort: MatchPersistencePort) : GetMatchesUseCase {
    override fun getAll(mode: String): List<MatchResult> =
        matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode)).map { MatchResult.from(it) }
}

fun modeToQueueIds(mode: String): List<Int> = when (mode) {
    "aram" -> listOf(3270)
    "all"  -> listOf(0, 3130, 3270)
    else   -> listOf(0, 3130)  // normal (기본): 칼바람 제외
}
