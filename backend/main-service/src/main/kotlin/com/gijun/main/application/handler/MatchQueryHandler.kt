package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetMatchPageQuery
import com.gijun.main.application.dto.result.MatchPageResult
import com.gijun.main.application.dto.result.MatchResult
import com.gijun.main.application.dto.result.MatchSummaryResult
import com.gijun.main.application.port.`in`.GetMatchPageUseCase
import com.gijun.main.application.port.`in`.GetMatchUseCase
import com.gijun.main.application.port.`in`.GetMatchesUseCase
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.domain.vo.MatchId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

private const val MAX_PAGE_SIZE = 100

/**
 * 경기 목록·단건 조회.
 */
@Service
@Transactional(readOnly = true)
class MatchQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
) : GetMatchesUseCase,
    GetMatchUseCase,
    GetMatchPageUseCase {
    override fun getMatches(mode: GameMode): List<MatchResult> =
        matchQueryPersistencePort.findAllWithParticipants(mode.queueIds).map { MatchResult.from(it) }

    override fun getMatch(matchId: MatchId): MatchResult? = matchQueryPersistencePort.findByMatchId(matchId)?.let { MatchResult.from(it) }

    override fun getMatchPage(query: GetMatchPageQuery): MatchPageResult {
        val safePage = query.page.coerceAtLeast(0)
        val safeSize = query.size.coerceIn(1, MAX_PAGE_SIZE)
        val queueIds = query.mode.queueIds

        val total = matchQueryPersistencePort.countByQueueIds(queueIds)
        val matches =
            matchQueryPersistencePort
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
