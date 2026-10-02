package com.gijun.main.domain.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.domain.model.match.MapPoint
import com.gijun.main.domain.model.match.MatchTimeline
import com.gijun.main.domain.model.match.ParticipantFrame
import com.gijun.main.domain.model.match.TimelineEvent
import com.gijun.main.domain.model.match.TimelineFrame
import org.slf4j.LoggerFactory

/**
 * 저장해 둔 `game-timelines` 원본 JSON 에서 [MatchTimeline] 을 뽑는다.
 *
 * 원본은 가공하지 않고 통째로 보관하고, 해석은 전부 여기서 한다. 나중에 다른 필드가
 * 필요해지면 저장 형식을 바꾸지 않고 이 파서만 늘리면 된다.
 *
 * 어떤 이유로든 읽을 수 없으면 빈 타임라인을 돌려준다 — 그러면 호출부가 자연히
 * [com.gijun.main.domain.model.match.LaneMethod.LEGACY_FINAL] 로 내려간다. 여기서 예외를 던져
 * 재집계 전체를 멈추게 하지 않는다.
 *
 * ## 원본에 있지만 **일부러 읽지 않는** 필드 (되살리지 마라)
 *
 * - `teamScore`, `dominionScore` — 다른 게임 모드의 잔재다. 17판 전수에서 항상 0이었다.
 * - `itemId`, `skillSlot` — 이벤트가 union 스키마라 필드 자리만 있다. 이 값을 채워 줄
 *   `ITEM_PURCHASED` / `SKILL_LEVEL_UP` 이벤트 자체가 LCU 에 없어서 항상 0이다.
 *   아이템 빌드 순서나 스킬 선마를 만들려면 수집 소스를 Riot match-v5 로 바꿔야 한다.
 * - `damageStats`, `championStats` — LCU 프레임에는 아예 없다(Riot match-v5 와 다른 점).
 *   그래서 분당 DPM 곡선은 이 데이터로 만들 수 없다.
 * - 이벤트의 `participantId` — 세 타입 모두 `killerId` / `victimId` 로 충분하고,
 *   union 스키마상 어느 쪽을 가리키는지가 타입마다 달라 혼동만 부른다.
 */
object TimelineParser {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper()

    val EMPTY = MatchTimeline(emptyList())

    /**
     * 타임라인을 몇 경기씩 묶어 읽을지. 경기당 60KB 급이라 500경기를 한 번에 들면 30MB 가
     * 힙에 얹히고, 파싱 중에는 그만큼의 JsonNode 트리가 같이 살아 있다.
     *
     * 원본을 여러 경기 읽는 코드는 **전부** 이 값으로 청크를 나눈다. 예전에는 호출부마다
     * 각자 상수를 복제했고, 그래서 조회 경로 한 곳이 청크 없이 전체를 들어올리고 있었다.
     */
    const val CHUNK_SIZE = 50

    fun parse(raw: String?): MatchTimeline {
        if (raw.isNullOrBlank()) return EMPTY
        return try {
            val frames = mapper.readTree(raw)["frames"] ?: return EMPTY
            MatchTimeline(
                frames = frames.mapNotNull(::frame).sortedBy { it.timestampMs },
                events =
                    frames
                        .flatMap { f -> f["events"]?.mapNotNull(::event).orEmpty() }
                        .sortedBy { it.timestampMs },
            )
        } catch (e: Exception) {
            log.warn("타임라인 파싱 실패 — LEGACY_FINAL 로 처리한다: ${e.message}")
            EMPTY
        }
    }

    /** 모르는 종류는 버린다. 이벤트 하나가 이상하다고 타임라인 전체를 버리지 않는다. */
    private fun event(node: JsonNode): TimelineEvent? {
        val ts = node["timestamp"]?.asLong() ?: return null
        val killerId = node["killerId"]?.asInt() ?: 0
        val at = point(node["position"])
        val assists = node["assistingParticipantIds"]?.map { it.asInt() }.orEmpty()
        return when (node["type"]?.asText()) {
            "CHAMPION_KILL" ->
                TimelineEvent.ChampionKill(
                    timestampMs = ts,
                    killerId = killerId,
                    victimId = node["victimId"]?.asInt() ?: return null,
                    assistIds = assists,
                    position = at,
                )
            "ELITE_MONSTER_KILL" ->
                TimelineEvent.EliteMonsterKill(
                    timestampMs = ts,
                    killerId = killerId,
                    monsterType = node["monsterType"]?.asText().orEmpty(),
                    monsterSubType = node["monsterSubType"]?.asText().orEmpty(),
                    assistIds = assists,
                    position = at,
                )
            "BUILDING_KILL" ->
                TimelineEvent.BuildingKill(
                    timestampMs = ts,
                    killerId = killerId,
                    buildingTeamId = node["teamId"]?.asInt() ?: 0,
                    buildingType = node["buildingType"]?.asText().orEmpty(),
                    towerType = node["towerType"]?.asText().orEmpty(),
                    laneType = node["laneType"]?.asText().orEmpty(),
                    assistIds = assists,
                    position = at,
                )
            else -> null
        }
    }

    /**
     * 좌표. 없으면 null 이다.
     *
     * (0,0) 도 null 로 본다 — 협곡의 밟히는 영역은 x,y 가 130 부터 시작한다(17판 실측).
     * 원점은 좌표가 아니라 "값이 안 왔다"는 신호로 다루는 쪽이 안전하다.
     */
    private fun point(node: JsonNode?): MapPoint? {
        if (node == null || node.isNull) return null
        val x = node["x"]?.asInt() ?: return null
        val y = node["y"]?.asInt() ?: return null
        return if (x == 0 && y == 0) null else MapPoint(x, y)
    }

    private fun frame(node: JsonNode): TimelineFrame? {
        val timestamp = node["timestamp"]?.asLong() ?: return null
        val pf = node["participantFrames"] ?: return null

        // participantFrames 는 "1".."10" 을 키로 하는 객체다. 배열로 오는 변종도 같이 받아 둔다.
        val entries =
            when {
                pf.isObject -> pf.fields().asSequence().map { it.value }
                pf.isArray -> pf.asSequence()
                else -> return null
            }

        val participants =
            entries
                .mapNotNull { p ->
                    val id = p["participantId"]?.asInt() ?: return@mapNotNull null
                    if (id <= 0) return@mapNotNull null
                    id to
                        ParticipantFrame(
                            participantId = id,
                            totalGold = p["totalGold"]?.asInt() ?: 0,
                            xp = p["xp"]?.asInt() ?: 0,
                            level = p["level"]?.asInt() ?: 0,
                            minionsKilled = p["minionsKilled"]?.asInt() ?: 0,
                            jungleMinionsKilled = p["jungleMinionsKilled"]?.asInt() ?: 0,
                            currentGold = p["currentGold"]?.asInt() ?: 0,
                            position = point(p["position"]),
                        )
                }.toMap()

        return if (participants.isEmpty()) null else TimelineFrame(timestamp, participants)
    }
}
