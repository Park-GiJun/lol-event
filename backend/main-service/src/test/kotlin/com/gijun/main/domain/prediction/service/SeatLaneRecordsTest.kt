package com.gijun.main.domain.prediction.service

import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.match.model.MatchParticipantModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 여기의 기댓값은 `lol-ml` 의 `FeatureBuilderTest` 가 같은 상황에서 기대하는 값과 같아야 한다.
 * 한쪽 공식을 고치면 양쪽 테스트가 같이 바뀌어야 한다.
 */
class SeatLaneRecordsTest {
    private val lanes = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)
    private val prior = 10.0

    private fun player(
        riotId: String,
        teamId: Int,
        position: String,
        win: Boolean,
        gold: Int = 10_000,
        damageSelfMitigated: Int = 0,
        puuid: String? = null,
    ) = MatchParticipantModel(
        puuid = puuid,
        riotId = riotId,
        champion = "Garen",
        team = if (teamId == 100) "blue" else "red",
        teamId = teamId,
        win = win,
        gold = gold,
        damageSelfMitigated = damageSelfMitigated,
        assignedPosition = position,
    )

    /** a1~a5 가 블루, b1~b5 가 레드. 적힌 순서대로 탑·정글·미드·원딜·서포터. 레드의 골드는 10,000 이다. */
    private fun match(
        id: String,
        blueWin: Boolean = true,
        blueGold: Int = 12_000,
        gameCreation: Long = id.hashCode().toLong(),
        gameDuration: Int = 1_800,
        participants: List<MatchParticipantModel>? = null,
    ) = MatchModel(
        matchId = id,
        queueId = 3130,
        gameCreation = gameCreation,
        gameDuration = gameDuration,
        participants =
            (
                participants
                    ?: (
                        lanes.mapIndexed { i, lane -> player("a${i + 1}", 100, lane.name, blueWin, gold = blueGold) } +
                            lanes.mapIndexed { i, lane -> player("b${i + 1}", 200, lane.name, !blueWin) }
                    )
            ).toMutableList(),
    )

    @Test
    fun `기록이 없는 사람은 어느 자리든 반반이다`() {
        val records = SeatLaneRecords.of(emptyList())

        assertEquals(0.5, records.seatLaneWinRate("손님#KR1", Position.TOP, prior))
        assertEquals(0, records.seatDuels("손님#KR1", Position.TOP))
    }

    @Test
    fun `라인 1승은 전체 승률을 올리고, 그 자리 승률을 그보다 더 올린다`() {
        val records = SeatLaneRecords.of(listOf(match("m1")))

        // lol-ml: laneWinRate = (1 + 10 × 0.5) / (1 + 10)
        assertEquals(6.0 / 11.0, records.laneWinRate("a1", prior), 1e-12)
        assertEquals(5.0 / 11.0, records.laneWinRate("b1", prior), 1e-12)
        // seatLaneWinRate = (1 + 10 × laneWinRate) / (1 + 10)
        assertEquals((1.0 + 10.0 * 6.0 / 11.0) / 11.0, records.seatLaneWinRate("a1", Position.TOP, prior), 1e-12)
        // 안 가 본 자리는 전체 라인 승률 그대로다.
        assertEquals(6.0 / 11.0, records.seatLaneWinRate("a1", Position.MID, prior), 1e-12)
        assertEquals(1, records.seatDuels("a1", Position.TOP))
        assertEquals(0, records.seatDuels("a1", Position.MID))
    }

    @Test
    fun `팀이 져도 라인에서 더 컸으면 라인은 이긴 것이다`() {
        val records = SeatLaneRecords.of(listOf(match("m1", blueWin = false, blueGold = 12_000)))

        assertEquals(6.0 / 11.0, records.laneWinRate("a1", prior), 1e-12)
    }

    @Test
    fun `피해감소는 골드 0_007 로 쳐서 탱커의 골드 열세를 메운다`() {
        val roster =
            lanes.mapIndexed { i, lane ->
                // 탑만 골드가 500 적고 피해감소가 100,000 많다: 9,500 + 700 > 10,000.
                if (i == 0) {
                    player("a1", 100, lane.name, true, gold = 9_500, damageSelfMitigated = 100_000)
                } else {
                    player("a${i + 1}", 100, lane.name, true)
                }
            } + lanes.mapIndexed { i, lane -> player("b${i + 1}", 200, lane.name, false) }

        val records = SeatLaneRecords.of(listOf(match("m1", participants = roster)))

        assertEquals(1, records.seatDuels("a1", Position.TOP))
        assertEquals(6.0 / 11.0, records.laneWinRate("a1", prior), 1e-12)
        // 나머지 네 자리는 점수가 같아서 맞대결이 성립하지 않는다.
        assertEquals(0, records.seatDuels("a2", Position.JUNGLE))
    }

    @Test
    fun `포지션 표기가 달라도 같은 자리로 묶는다`() {
        val roster =
            lanes.mapIndexed { i, lane ->
                player(
                    "a${i + 1}",
                    100,
                    if (lane ==
                        Position.MID
                    ) {
                        "MIDDLE"
                    } else {
                        lane.name
                    },
                    true,
                    gold = 12_000,
                )
            } +
                lanes.mapIndexed { i, lane -> player("b${i + 1}", 200, lane.name, false) }

        val records = SeatLaneRecords.of(listOf(match("m1", participants = roster)))

        assertEquals(1, records.seatDuels("a3", Position.MID))
        assertEquals(1, records.seatDuels("b3", Position.MID))
    }

    @Test
    fun `짧은 경기, 5대5 가 아닌 경기, 두 번 들어온 같은 경기는 세지 않는다`() {
        val short = match("short", gameDuration = 599)
        val fourVsFive = match("4v5").let { it.copy(participants = it.participants.drop(1).toMutableList()) }
        val once = match("once", gameCreation = 1_000L)
        val again = match("again", gameCreation = 1_000L)

        val records = SeatLaneRecords.of(listOf(short, fourVsFive, once, again))

        assertEquals(1, records.seatDuels("a1", Position.TOP))
    }

    @Test
    fun `Riot ID 를 바꿔도 puuid 가 같으면 한 사람의 기록이다`() {
        fun roster(topName: String) =
            lanes.mapIndexed { i, lane ->
                player(if (i == 0) topName else "a${i + 1}", 100, lane.name, true, gold = 12_000, puuid = "puuid-a${i + 1}")
            } + lanes.mapIndexed { i, lane -> player("b${i + 1}", 200, lane.name, false, puuid = "puuid-b${i + 1}") }

        val records =
            SeatLaneRecords.of(
                listOf(match("m1", participants = roster("옛이름#KR1")), match("m2", participants = roster("새이름#KR1"))),
            )

        assertEquals(2, records.seatDuels("새이름#KR1", Position.TOP))
        assertEquals(2, records.seatDuels("옛이름#KR1", Position.TOP))
        assertEquals(records.personKey("옛이름#KR1"), records.personKey("새이름#KR1"))
        // puuid 가 없는 옛 기록은 Riot ID 가 그대로 키다.
        assertEquals("손님#KR1", records.personKey("손님#KR1"))
    }

    @Test
    fun `한 팀에 같은 자리가 둘이면 그 자리는 버린다`() {
        val roster =
            listOf(
                player("a1", 100, "TOP", true, gold = 12_000),
                player("a2", 100, "TOP", true, gold = 12_000),
                player("a3", 100, "MID", true, gold = 12_000),
                player("a4", 100, "ADC", true, gold = 12_000),
                player("a5", 100, "SUPPORT", true, gold = 12_000),
            ) + lanes.mapIndexed { i, lane -> player("b${i + 1}", 200, lane.name, false) }

        val records = SeatLaneRecords.of(listOf(match("m1", participants = roster)))

        assertEquals(0, records.seatDuels("a1", Position.TOP))
        assertEquals(0, records.seatDuels("b2", Position.JUNGLE))
        assertEquals(1, records.seatDuels("a3", Position.MID))
    }
}
