package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.match.result.MatchPageResult
import com.gijun.main.application.dto.match.result.MatchResult
import com.gijun.main.application.dto.match.result.MatchSummaryResult
import com.gijun.main.application.port.`in`.GetMatchesUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

private const val MAX_PAGE_SIZE = 100

@Service
@Transactional(readOnly = true)
class GetMatchesHandler(private val matchPersistencePort: MatchPersistencePort) : GetMatchesUseCase {
    override fun getAll(mode: String): List<MatchResult> =
        matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode)).map { MatchResult.from(it) }

    override fun getById(matchId: String): MatchResult? =
        matchPersistencePort.findByMatchId(matchId)?.let { MatchResult.from(it) }

    override fun getPage(mode: String, page: Int, size: Int): MatchPageResult {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val queueIds = modeToQueueIds(mode)

        val total = matchPersistencePort.countByQueueIds(queueIds)
        val matches = matchPersistencePort
            .findPageWithParticipants(queueIds, safePage, safeSize)
            .map { MatchSummaryResult.from(it) }

        val totalPages = ceil(total.toDouble() / safeSize).toInt()
        return MatchPageResult(
            matches = matches,
            page = safePage,
            size = safeSize,
            totalElements = total,
            totalPages = totalPages,
            hasNext = safePage + 1 < totalPages,
        )
    }
}

fun modeToQueueIds(mode: String): List<Int> = when (mode) {
    "aram" -> listOf(3270)
    "all"  -> listOf(0, 3130)  // 칼바람(3270) 무조건 제외 — 통계 집계에서 항상 배제
    else   -> listOf(0, 3130)  // normal (기본)
}
