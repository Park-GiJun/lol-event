package com.gijun.main.domain.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.domain.model.match.MatchTimeline
import com.gijun.main.domain.model.match.ParticipantFrame
import com.gijun.main.domain.model.match.TimelineEvent
import com.gijun.main.domain.model.match.TimelineFrame
import org.slf4j.LoggerFactory

/**
 * 저장해 둔 `game-timelines` 원본 JSON 에서 [MatchTimeline] 을 뽑는다.
 *
 * 원본은 가공하지 않고 통째로 보관하고, 해석은 전부 여기서 한다. 나중에 이벤트나 좌표가
 * 필요해지면 저장 형식을 바꾸지 않고 이 파서만 늘리면 된다.
 *
 * 어떤 이유로든 읽을 수 없으면 빈 타임라인을 돌려준다 — 그러면 호출부가 자연히
 * [com.gijun.main.domain.model.match.LaneMethod.LEGACY_FINAL] 로 내려간다. 여기서 예외를 던져
 * 재집계 전체를 멈추게 하지 않는다.
 */
object TimelineParser {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper()

    val EMPTY = MatchTimeline(emptyList())

    fun parse(raw: String?): MatchTimeline {
        if (raw.isNullOrBlank()) return EMPTY
        return try {
            val frames = mapper.readTree(raw)["frames"] ?: return EMPTY
            MatchTimeline(
                frames = frames.mapNotNull(::frame).sortedBy { it.timestampMs },
                events = frames.flatMap { f -> f["events"]?.mapNotNull(::event).orEmpty() }
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
        return when (node["type"]?.asText()) {
            "CHAMPION_KILL" -> TimelineEvent.ChampionKill(
                timestampMs = ts,
                killerId = killerId,
                victimId = node["victimId"]?.asInt() ?: return null,
                assistIds = node["assistingParticipantIds"]?.map { it.asInt() }.orEmpty(),
            )
            "ELITE_MONSTER_KILL" -> TimelineEvent.EliteMonsterKill(
                timestampMs = ts,
                killerId = killerId,
                monsterType = node["monsterType"]?.asText().orEmpty(),
            )
            "BUILDING_KILL" -> TimelineEvent.BuildingKill(
                timestampMs = ts,
                killerId = killerId,
                buildingTeamId = node["teamId"]?.asInt() ?: 0,
                buildingType = node["buildingType"]?.asText().orEmpty(),
            )
            else -> null
        }
    }

    private fun frame(node: JsonNode): TimelineFrame? {
        val timestamp = node["timestamp"]?.asLong() ?: return null
        val pf = node["participantFrames"] ?: return null

        // participantFrames 는 "1".."10" 을 키로 하는 객체다. 배열로 오는 변종도 같이 받아 둔다.
        val entries = when {
            pf.isObject -> pf.fields().asSequence().map { it.value }
            pf.isArray  -> pf.asSequence()
            else        -> return null
        }

        val participants = entries.mapNotNull { p ->
            val id = p["participantId"]?.asInt() ?: return@mapNotNull null
            if (id <= 0) return@mapNotNull null
            id to ParticipantFrame(
                participantId = id,
                totalGold = p["totalGold"]?.asInt() ?: 0,
                xp = p["xp"]?.asInt() ?: 0,
                level = p["level"]?.asInt() ?: 0,
                minionsKilled = p["minionsKilled"]?.asInt() ?: 0,
                jungleMinionsKilled = p["jungleMinionsKilled"]?.asInt() ?: 0,
            )
        }.toMap()

        return if (participants.isEmpty()) null else TimelineFrame(timestamp, participants)
    }
}
