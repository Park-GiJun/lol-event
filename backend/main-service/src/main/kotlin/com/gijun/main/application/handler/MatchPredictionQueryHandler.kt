package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.DescribeMatchPredictionQuery
import com.gijun.main.application.port.`in`.DescribeMatchPredictionUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.external.PlayerTierKtorPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.WinModelQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.prediction.model.WinModel
import com.gijun.main.domain.prediction.service.SeatLaneRecords
import org.springframework.stereotype.Service

/**
 * 픽 전 승률 예측. 챗봇 tool 이 부른다.
 *
 * **확률은 [WinModel] 이 낸다.** LLM 은 여기서 쓴 글을 옮기기만 한다.
 *
 * 레이팅과 편성에는 손대지 않는다. 경기 기록에서 라인 맞대결을 따로 세고([SeatLaneRecords]), 티어는
 * Riot API 에서 읽는다. 편성 화면의 기대 승률(자리 Elo)은 이 계산과 무관하게 그대로다.
 *
 * **`@Transactional` 을 달지 않는다.** 열 명의 티어를 Riot API 에 묻는 동안 DB 커넥션을 쥐고 있게 된다.
 */
@Service
class MatchPredictionQueryHandler(
    private val winModelQueryPersistencePort: WinModelQueryPersistencePort,
    private val getTeamCandidatesUseCase: GetTeamCandidatesUseCase,
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
    private val playerTierKtorPort: PlayerTierKtorPort,
) : DescribeMatchPredictionUseCase {
    override fun describeMatchPrediction(query: DescribeMatchPredictionQuery): String {
        val asked = (query.blue + query.red).map { it.trim() }
        if (query.blue.size != LANES.size || query.red.size != LANES.size || asked.any { it.isEmpty() }) {
            return "팀마다 다섯 명이 필요하다(탑, 정글, 미드, 원딜, 서포터). 빠진 자리를 사용자에게 물어본다."
        }

        val known = getTeamCandidatesUseCase.getTeamCandidates().map { it.riotId }
        val matches = asked.associateWith { PlayerNames.match(it, known) }
        val problems =
            matches.mapNotNull { (name, found) ->
                when {
                    found.isEmpty() -> "'$name' 이라는 플레이어를 찾지 못했다"
                    found.size > 1 -> "'$name' 에 맞는 사람이 여럿이다(${found.joinToString(", ")})"
                    else -> null
                }
            }
        if (problems.isNotEmpty()) return problems.joinToString("; ") + ". $NO_GUESSING"

        val riotIds = asked.map { matches.getValue(it).single() }
        val records = seatLaneRecords()
        // Riot ID 를 바꾼 사람은 옛 이름과 새 이름이 같은 사람이다.
        val repeated = riotIds.groupBy { records.personKey(it) }.values.filter { it.size > 1 }
        if (repeated.isNotEmpty()) {
            return "같은 사람이 두 번 들어갔다: ${repeated.joinToString("; ") { it.distinct().joinToString(" = ") }}. 열 명이 전부 달라야 한다."
        }

        val winModel = winModelQueryPersistencePort.find()
        val seats =
            riotIds.mapIndexed { index, riotId ->
                val position = LANES[index % LANES.size]
                PredictionSeat(
                    riotId = riotId,
                    position = position,
                    seatLaneWinRate = records.seatLaneWinRate(riotId, position, winModel.shrinkPrior),
                    seatDuels = records.seatDuels(riotId, position),
                    tier = playerTierKtorPort.findTier(riotId),
                )
            }
        return MatchPredictionWriter.write(winModel, seats.take(LANES.size), seats.drop(LANES.size))
    }

    /** 경기가 저장되면 통계 캐시와 같이 비워진다. */
    private fun seatLaneRecords(): SeatLaneRecords =
        statsResultCacheQueryPort.getOrCompute("seat-lane-records") {
            SeatLaneRecords.of(matchQueryPersistencePort.findAllWithParticipants(GameMode.NORMAL.queueIds))
        }

    private companion object {
        /** 이름을 받는 순서. tool 의 인자 순서와 같다. */
        val LANES = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

        const val NO_GUESSING = "비슷한 이름을 지어내 넣지 않는다. 이름을 다시 확인해 달라고만 한다."
    }
}
