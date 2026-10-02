package com.gijun.main.domain.match.service

import com.gijun.main.domain.match.model.MapPoint
import com.gijun.main.domain.match.model.TimelineEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 원본 JSON → [com.gijun.main.domain.match.model.MatchTimelineModel] 변환만 본다.
 * 지표 계산은 [TimelineMetricsTest] 가 고정한다.
 *
 * 픽스처의 JSON 은 저장된 원본 모양을 따랐다 — 이벤트는 타입별 스키마가 아니라 15개 필드
 * 공용 union 이라, 타입에 안 맞는 필드까지 같이 실어 보낸다.
 */
class TimelineParserTest {
    // ────────── 픽스처 ──────────

    /** 프레임 하나에 [events] 를 담은 최소 원본. */
    private fun raw(
        vararg events: String,
        frame: String = participantFrame(1),
    ): String = """{"frames":[{"timestamp":60000,"participantFrames":{$frame},"events":[${events.joinToString(",")}]}]}"""

    private fun participantFrame(
        pid: Int,
        currentGold: Int = 340,
        position: String = """"position":{"x":603,"y":611},""",
    ) = """"$pid":{"participantId":$pid,"totalGold":2500,"currentGold":$currentGold,"xp":1800,""" +
        """"level":4,"minionsKilled":32,"jungleMinionsKilled":0,$position"teamScore":0,"dominionScore":0}"""

    /** 실제 원본처럼 union 의 모든 필드를 채워 보낸다. 타입에 안 맞는 값은 파서가 버려야 한다. */
    private fun event(
        type: String,
        ts: Long = 600_000,
        x: Int = 7_000,
        y: Int = 7_000,
        extra: String = "",
    ) = """{"type":"$type","timestamp":$ts,"position":{"x":$x,"y":$y},""" +
        """"killerId":1,"victimId":6,"participantId":0,"assistingParticipantIds":[2,3],""" +
        """"itemId":0,"skillSlot":0,$extra"teamId":200}"""

    private fun parse(vararg events: String) = TimelineParser.parse(raw(*events))

    private inline fun <reified T : TimelineEvent> parseOne(event: String): T = parse(event).events.single() as T

    // ────────── 이벤트 ──────────

    @Test
    fun `챔피언 킬은 킬러 희생자 어시스트와 좌표를 읽는다`() {
        val kill = parseOne<TimelineEvent.ChampionKill>(event("CHAMPION_KILL", ts = 123_456, x = 4_200, y = 9_100))

        assertEquals(123_456, kill.timestampMs)
        assertEquals(1, kill.killerId)
        assertEquals(6, kill.victimId)
        assertEquals(listOf(2, 3), kill.assistIds)
        assertEquals(MapPoint(4_200, 9_100), kill.position)
    }

    @Test
    fun `오브젝트 킬은 드래곤 원소와 관여자를 읽는다`() {
        val monster =
            parseOne<TimelineEvent.EliteMonsterKill>(
                event("ELITE_MONSTER_KILL", x = 9_910, y = 4_530, extra = """"monsterType":"DRAGON","monsterSubType":"FIRE_DRAGON","""),
            )

        assertEquals("DRAGON", monster.monsterType)
        assertEquals("FIRE_DRAGON", monster.monsterSubType)
        assertEquals(listOf(2, 3), monster.assistIds, "오브젝트도 관여자를 준다 — 예전엔 킬에만 썼다")
        assertEquals(MapPoint(9_910, 4_530), monster.position)
    }

    @Test
    fun `건물 킬은 포탑 종류와 라인을 읽는다`() {
        val building =
            parseOne<TimelineEvent.BuildingKill>(
                event("BUILDING_KILL", extra = """"buildingType":"TOWER_BUILDING","towerType":"OUTER_TURRET","laneType":"TOP_LANE","""),
            )

        assertEquals("TOWER_BUILDING", building.buildingType)
        assertEquals("OUTER_TURRET", building.towerType)
        assertEquals("TOP_LANE", building.laneType)
        assertEquals(200, building.buildingTeamId, "teamId 는 부서진 쪽의 팀이다")
    }

    @Test
    fun `억제기는 포탑 종류가 빈 값이다`() {
        val inhibitor =
            parseOne<TimelineEvent.BuildingKill>(
                event("BUILDING_KILL", extra = """"buildingType":"INHIBITOR_BUILDING","towerType":"","laneType":"MID_LANE","""),
            )

        assertEquals("INHIBITOR_BUILDING", inhibitor.buildingType)
        assertEquals("", inhibitor.towerType)
    }

    @Test
    fun `union 스키마에서 타입에 안 맞는 필드는 무시한다`() {
        // 아이템·스킬 필드가 실려 오지만 그 이벤트 타입 자체가 LCU 에 없어 항상 0이다.
        val kill =
            parseOne<TimelineEvent.ChampionKill>(
                event("CHAMPION_KILL", extra = """"monsterType":"DRAGON","towerType":"OUTER_TURRET","laneType":"TOP_LANE","""),
            )

        assertEquals(1, kill.killerId, "다른 타입의 필드가 섞여 있어도 킬은 정상 파싱된다")
    }

    @Test
    fun `모르는 이벤트 타입은 버리고 나머지는 살린다`() {
        val timeline =
            parse(
                event("WARD_PLACED"),
                event("CHAMPION_KILL", ts = 300_000),
                event("ITEM_PURCHASED"),
            )

        assertEquals(1, timeline.events.size, "LCU 에 없는 타입이 섞여 와도 아는 것만 남긴다")
        assertTrue(timeline.events.single() is TimelineEvent.ChampionKill)
    }

    @Test
    fun `이벤트는 프레임에 흩어져 있어도 시간순으로 펴진다`() {
        val raw = """{"frames":[
            {"timestamp":60000,"participantFrames":{${participantFrame(1)}},"events":[${event("CHAMPION_KILL", ts = 50_000)}]},
            {"timestamp":120000,"participantFrames":{${participantFrame(1)}},"events":[${event("CHAMPION_KILL", ts = 20_000)}]}
        ]}"""

        assertEquals(listOf(20_000L, 50_000L), TimelineParser.parse(raw).events.map { it.timestampMs })
    }

    // ────────── 좌표 ──────────

    @Test
    fun `좌표가 없으면 null 이다`() {
        val kill =
            parseOne<TimelineEvent.ChampionKill>(
                """{"type":"CHAMPION_KILL","timestamp":600000,"killerId":1,"victimId":6,"assistingParticipantIds":[]}""",
            )

        assertNull(kill.position)
    }

    @Test
    fun `원점은 좌표가 아니라 결손으로 본다`() {
        // 협곡의 밟히는 영역은 x,y 가 130 부터다. (0,0) 을 좌표로 세면 비율 지표의 분모가 오염된다.
        assertNull(parseOne<TimelineEvent.ChampionKill>(event("CHAMPION_KILL", x = 0, y = 0)).position)
    }

    // ────────── 프레임 ──────────

    @Test
    fun `프레임의 보유 골드와 좌표를 읽는다`() {
        val frame =
            parse()
                .frames
                .single()
                .participants
                .getValue(1)

        assertEquals(340, frame.currentGold, "누적 골드가 아니라 손에 든 골드다")
        assertEquals(2_500, frame.totalGold)
        assertEquals(MapPoint(603, 611), frame.position)
    }

    @Test
    fun `프레임에 좌표가 없어도 나머지는 읽는다`() {
        val timeline = TimelineParser.parse(raw(frame = participantFrame(1, position = "")))
        val frame =
            timeline.frames
                .single()
                .participants
                .getValue(1)

        assertNull(frame.position)
        assertEquals(32, frame.cs)
    }

    // ────────── 결손 ──────────

    @Test
    fun `망가진 JSON 은 빈 타임라인이다`() {
        assertTrue(TimelineParser.parse("{ 이건 JSON 이 아니다").isEmpty)
        assertTrue(TimelineParser.parse("").isEmpty)
        assertTrue(TimelineParser.parse(null).isEmpty)
        assertTrue(TimelineParser.parse("""{"frames":[]}""").isEmpty)
    }
}
