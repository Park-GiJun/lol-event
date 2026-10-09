package com.gijun.main.domain.prediction.service

import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.match.model.MatchParticipantModel
import com.gijun.main.domain.rating.service.SeatRatings

/**
 * 사람 × 자리별 **라인 맞대결 전적**. 승률 예측 모델([com.gijun.main.domain.prediction.model.WinModel])의 입력이다.
 *
 * ## lol-ml 과 같은 공식이어야 한다
 * 모델의 계수는 `lol-ml` 이 자기 방식으로 만든 피처에 맞춰 학습됐다. 여기서 다르게 세면 계수가 뜻을 잃는다.
 * 그래서 이 클래스는 `lol-ml` 의 `LaneDuels` · `PlayerState.seatLaneWinRate` · `MatchCleaner` 를 그대로 옮긴 것이다.
 * 한쪽을 고치면 다른 쪽도 고친다.
 *
 * **레이팅과는 따로 센다.** 레이팅 이력의 라인 결과는 타임라인이 있으면 15분 시점으로 승자를 가리는데
 * (`LaneScores`), 모델은 타임라인 없이 경기 종료 시점 값으로만 학습됐다. 이력을 가져다 쓰면 승자가 달라진다.
 * 이 클래스는 Elo 를 읽지도 바꾸지도 않는다.
 */
class SeatLaneRecords private constructor(
    /** 사람 키 → 자리 → 누적. */
    private val cells: Map<String, Map<Position, Cell>>,
    /** Riot ID → 사람 키. 한 사람이 Riot ID 를 바꿨으면 여러 Riot ID 가 같은 키를 가리킨다. */
    private val keys: Map<String, String>,
) {
    private class Cell {
        var duels = 0
        var wins = 0
    }

    /**
     * 사람을 가르는 키. `lol-ml` 과 같이 puuid 를 쓰고, 비어 있는 옛 기록만 Riot ID 로 대신한다.
     * Riot ID 를 바꾼 사람의 기록이 둘로 갈리지 않게 하려는 것이다. 기록이 없는 사람은 Riot ID 그대로다.
     */
    fun personKey(riotId: String): String = keys[riotId] ?: riotId

    /** 그 자리에서 성립한 라인 맞대결 수. */
    fun seatDuels(
        riotId: String,
        position: Position,
    ): Int = cells[personKey(riotId)]?.get(position)?.duels ?: 0

    /** 자리를 가리지 않은 라인 맞대결 승률. 기록이 없으면 0.5. */
    fun laneWinRate(
        riotId: String,
        prior: Double,
    ): Double {
        val mine = cells[personKey(riotId)]?.values.orEmpty()
        return shrunk(mine.sumOf { it.wins }, mine.sumOf { it.duels }, prior, HALF)
    }

    /**
     * 이 자리에서의 라인 맞대결 승률. 표본이 적으면 그 사람의 전체 라인 승률 쪽으로 당긴다 —
     * 처음 앉는 자리는 "평소만큼 한다" 에서 출발한다.
     *
     * @param prior 당기는 세기. 모델과 같이 내보낸 값을 쓴다.
     */
    fun seatLaneWinRate(
        riotId: String,
        position: Position,
        prior: Double,
    ): Double {
        val cell = cells[personKey(riotId)]?.get(position)
        return shrunk(cell?.wins ?: 0, cell?.duels ?: 0, prior, laneWinRate(riotId, prior))
    }

    private fun shrunk(
        wins: Int,
        count: Int,
        prior: Double,
        fallback: Double,
    ): Double = (wins + prior * fallback) / (count + prior)

    companion object {
        /** 이보다 짧으면 다시하기·초반 AFK 로 본다. 레이팅 검증과 같은 기준이다. */
        const val MIN_DURATION_SEC = 600

        /** 피해감소 1 = 골드 0.007. `LaneScores.C_MIT` · lol-ml 의 `MITIGATION_WEIGHT` 와 같은 값이다. */
        const val MITIGATION_WEIGHT = 0.007

        private const val HALF = 0.5
        private const val BLUE_TEAM_ID = 100
        private const val TEAM_SIZE = 5

        /** @param matches 내전 큐의 경기. 순서는 상관없다 — 세기만 한다. */
        fun of(matches: List<MatchModel>): SeatLaneRecords {
            val cells = HashMap<String, MutableMap<Position, Cell>>()
            val keys = HashMap<String, String>()
            val seenGames = HashSet<Pair<Long, Set<String>>>()
            for (match in matches) {
                match.participants.forEach { keys[it.riotId] = keyOf(it) }
                if (match.gameDuration < MIN_DURATION_SEC || !isFiveVsFive(match)) continue
                // 같은 경기가 다른 matchId 로 두 번 들어온 경우. 시작 시각과 열 명이 같으면 같은 경기다.
                if (!seenGames.add(match.gameCreation to match.participants.map(::keyOf).toSet())) continue
                for ((position, winner, loser) in duels(match)) {
                    cells.getOrPut(winner) { HashMap() }.getOrPut(position) { Cell() }.also {
                        it.duels++
                        it.wins++
                    }
                    cells.getOrPut(loser) { HashMap() }.getOrPut(position) { Cell() }.duels++
                }
            }
            return SeatLaneRecords(cells, keys)
        }

        /** 팀마다 다섯, 열 명이 전부 다른 사람, 승패가 팀 안에서는 같고 팀끼리는 반대. */
        private fun isFiveVsFive(match: MatchModel): Boolean {
            val (blue, red) = match.participants.partition { it.teamId == BLUE_TEAM_ID }
            if (blue.size != TEAM_SIZE || red.size != TEAM_SIZE) return false
            if (match.participants
                    .map(::keyOf)
                    .toSet()
                    .size != TEAM_SIZE * 2
            ) {
                return false
            }
            val blueWin = blue.first().win
            return blue.all { it.win == blueWin } && red.all { it.win != blueWin }
        }

        /**
         * (자리, 이긴 사람, 진 사람). 같은 자리에 정확히 두 명이 서로 다른 팀으로 있을 때만 성립하고,
         * 점수가 같으면 버린다.
         */
        private fun duels(match: MatchModel): List<Triple<Position, String, String>> =
            match.participants
                .mapNotNull { participant -> positionOf(participant)?.let { it to participant } }
                .groupBy({ it.first }, { it.second })
                .mapNotNull { (position, pair) ->
                    if (pair.size != 2) return@mapNotNull null
                    val (a, b) = pair
                    val scoreA = score(a)
                    val scoreB = score(b)
                    when {
                        a.teamId == b.teamId || scoreA == scoreB -> null
                        scoreA > scoreB -> Triple(position, keyOf(a), keyOf(b))
                        else -> Triple(position, keyOf(b), keyOf(a))
                    }
                }

        /** `lol-ml` 의 `MatchExtractor` 와 같은 규칙이다. */
        private fun keyOf(participant: MatchParticipantModel): String = participant.puuid?.takeIf { it.isNotBlank() } ?: participant.riotId

        /** 두 사람의 경기 시간이 같아서 분당으로 나누지 않아도 비교 결과는 같다. */
        private fun score(participant: MatchParticipantModel): Double =
            participant.gold + MITIGATION_WEIGHT * participant.damageSelfMitigated

        private fun positionOf(participant: MatchParticipantModel): Position? =
            SeatRatings.normalize(participant.assignedPosition.trim())?.let { Position.valueOf(it) }
    }
}
