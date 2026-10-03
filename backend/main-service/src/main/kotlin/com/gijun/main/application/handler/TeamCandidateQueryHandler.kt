package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.TeamCandidatePositionResult
import com.gijun.main.application.dto.result.TeamCandidateResult
import com.gijun.main.application.port.`in`.GetMembersUseCase
import com.gijun.main.application.port.`in`.GetRatingsUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.rating.service.RatingMath
import com.gijun.main.domain.team.service.TeamBalancer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 편성 후보 — 내전 멤버와, 멤버는 아니어도 경기 기록이 있는 사람.
 *
 * **기본 가능 포지션**은 "그 자리를 충분히 가 봤는가" 로 정한다. 한두 판은 땜빵이었을 수 있어서
 * 뺀다. 예: 탑 53 · 미드 28 · 정글 18 · 원딜 14 · 서포터 0 인 사람은 서포터만 빼고 넷 다 켜진다.
 * 화면에서 사람이 이번 판에 갈 자리를 고쳐 보낼 수 있으므로, 이 값은 출발점일 뿐이다.
 */
@Service
@Transactional(readOnly = true)
class TeamCandidateQueryHandler(
    private val getMembersUseCase: GetMembersUseCase,
    private val getRatingsUseCase: GetRatingsUseCase,
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
) : GetTeamCandidatesUseCase {
    override fun getTeamCandidates(): List<TeamCandidateResult> {
        val lanes = TeamBalancer.LANES.map { it.name }
        val ratings = getRatingsUseCase.getRatings().associateBy { it.riotId }
        val positions =
            matchQueryPersistencePort
                .findPositionCounts()
                .filter { it.position in lanes }
                .groupBy { it.riotId }

        val riotIds = (getMembersUseCase.getMembers().map { it.riotId } + ratings.keys + positions.keys).distinct()

        return riotIds
            .map { riotId ->
                val played =
                    positions[riotId]
                        .orEmpty()
                        .map { TeamCandidatePositionResult(it.position, it.games.toInt()) }
                        .sortedByDescending { it.games }
                val familiar = played.filter { it.games >= MIN_GAMES_TO_COUNT }.map { it.position }
                TeamCandidateResult(
                    riotId = riotId,
                    elo = ratings[riotId]?.laneElo ?: RatingMath.START,
                    games = played.sumOf { it.games },
                    mainPosition = played.firstOrNull()?.position,
                    positions = played,
                    // 다섯 자리 순서로 돌려준다. 화면 버튼 순서와 같다.
                    defaultPositions = if (familiar.isEmpty()) lanes else lanes.filter { it in familiar },
                )
            }.sortedWith(compareByDescending<TeamCandidateResult> { it.games }.thenBy { it.riotId })
    }

    private companion object {
        /** 이 판수 미만은 "갈 수 있는 자리" 로 치지 않는다. */
        const val MIN_GAMES_TO_COUNT = 3
    }
}
