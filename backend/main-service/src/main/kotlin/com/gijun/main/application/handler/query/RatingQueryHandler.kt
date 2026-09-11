package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.EloHistoryEntry
import com.gijun.main.application.dto.stats.result.EloLeaderboardResult
import com.gijun.main.application.dto.stats.result.EloRankEntry
import com.gijun.main.application.dto.stats.result.PlayerEloHistoryResult
import com.gijun.main.application.port.`in`.GetEloHistoryUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetRatingUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.PlayerRatingPort
import com.gijun.main.application.port.out.RatingHistoryPort
import com.gijun.main.domain.model.rating.PlayerRating
import com.gijun.main.domain.service.RankingScore
import com.gijun.main.domain.service.RatingMath
import com.gijun.main.domain.service.RiotIdNormalizer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RatingQueryHandler(
    private val playerRatingPort: PlayerRatingPort,
    private val ratingHistoryPort: RatingHistoryPort,
    private val matchPersistencePort: MatchPersistencePort,
    private val normalizer: RiotIdNormalizer,
) : GetRatingUseCase, GetEloLeaderboardUseCase, GetEloHistoryUseCase {

    override fun getAll(): List<PlayerRating> = playerRatingPort.findAll()

    override fun getByRiotId(riotId: String): PlayerRating? =
        playerRatingPort.findByRiotId(normalizer.canonical(riotId))

    override fun getHistory(riotId: String, limit: Int): PlayerEloHistoryResult {
        val id = normalizer.canonical(riotId)
        val ranked = rankedOrder()
        val mine = ranked.firstOrNull { it.riotId == id }

        return PlayerEloHistoryResult(
            riotId = id,
            currentElo = mine?.laneElo ?: RatingMath.START,
            eloRank = ranked.indexOfFirst { it.riotId == id }.takeIf { it >= 0 }?.plus(1),
            history = ratingHistoryPort.findByRiotId(id, limit).map {
                EloHistoryEntry(
                    matchId = it.matchId,
                    eloBefore = it.laneBefore, eloAfter = it.laneAfter, delta = it.laneDelta,
                    win = it.win,
                    laneResult = it.laneResult.name, laneOpponent = it.laneOpponent,
                    teamEloBefore = it.teamBefore, teamEloAfter = it.teamAfter, teamDelta = it.teamDelta,
                    gameCreation = it.gameCreation,
                )
            },
        )
    }

    /**
     * 배치(placement) 개념을 둔다.
     *
     * 70명 중 상당수가 한두 경기만 뛰었는데, 이들을 100경기 뛴 사람과 같은 표에 같은 순위로
     * 세우면 리더보드가 의미를 잃는다. 목록에서 지우지는 않되 순위는 매기지 않고 뒤로 보낸다.
     *
     * 기준은 **경기 수가 아니라 라인 맞대결 수**다. 포지션이 깨진 경기나 칼바람은 맞대결이
     * 성립하지 않아 실력 표본이 되지 못하는데, 경기 수로 세면 그것까지 표본으로 쳐 버린다.
     */
    override fun getLeaderboard(minDuels: Int): EloLeaderboardResult {
        val mainPositions = mainPositions()
        val sorted = rankedOrder()
        val (ranked, placement) = sorted.partition { it.laneDuels >= minDuels }

        fun entry(r: PlayerRating, rank: Int) = EloRankEntry(
            rank = rank,
            riotId = r.riotId,
            laneElo = round2(r.laneElo),
            laneEloDisplay = round2(r.laneEloDisplay),
            laneDuels = r.laneDuels,
            laneWinRate = rate(r.laneWins, r.laneDuels),
            teamElo = round2(r.teamElo),
            teamEloDisplay = round2(r.teamEloDisplay),
            teamGames = r.teamGames,
            winRate = rate(r.teamWins, r.teamGames),
            gap = round2(r.gap),
            placement = rank == 0,
            mainPosition = mainPositions[r.riotId],
            elo = round2(r.laneElo),
            games = r.teamGames,
            wins = r.teamWins,
            losses = r.teamGames - r.teamWins,
            winStreak = r.teamWinStreak,
            lossStreak = r.teamLossStreak,
            sampleGrade = RankingScore.sampleGrade(r.laneDuels),
        )

        return EloLeaderboardResult(
            players = ranked.mapIndexed { idx, r -> entry(r, idx + 1) } + placement.map { entry(it, 0) },
            minDuels = minDuels,
            rankedCount = ranked.size,
            placementCount = placement.size,
        )
    }

    /** 정렬 기본값은 라인 레이팅 **표시값**이다. 원값으로 정렬하면 표본 적은 사람이 위로 튄다. */
    private fun rankedOrder(): List<PlayerRating> =
        playerRatingPort.findAll().sortedByDescending { it.laneEloDisplay }

    /** 가장 많이 뛴 포지션. 별명은 정규 이름으로 합쳐서 센다. */
    private fun mainPositions(): Map<String, String> =
        matchPersistencePort.findPositionCounts()
            .groupBy({ normalizer.canonical(it.riotId) }, { it.position to it.games })
            .mapValues { (_, rows) ->
                rows.groupBy({ it.first }, { it.second })
                    .mapValues { (_, counts) -> counts.sum() }
                    .maxByOrNull { it.value }?.key
            }
            .filterValues { it != null }
            .mapValues { it.value!! }

    private fun rate(part: Int, total: Int): Double =
        if (total <= 0) 0.0 else round3(part.toDouble() / total)

    private fun round2(v: Double) = Math.round(v * 100) / 100.0
    private fun round3(v: Double) = Math.round(v * 1000) / 1000.0
}
