package com.gijun.main.infrastructure.adapter.`in`.web.match.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gijun.main.application.dto.result.KillEntry
import com.gijun.main.application.dto.result.MapPointDto
import com.gijun.main.application.dto.result.MatchTimelineResult
import com.gijun.main.application.dto.result.ObjectiveEntry
import com.gijun.main.application.dto.result.ParticipantTimelineSeries
import com.gijun.main.application.dto.result.TeamFightEntry
import com.gijun.main.application.dto.result.TeamTimelineSeries
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "MatchTimelineResult")
data class MatchTimelineResponse(
    val matchId: String,
    @field:Schema(
        description =
            "타임라인이 있는 경기인가. false 면 아래 목록이 전부 비어 있다. **404 를 주지 않는 이유**: 타임라인은 새 수집기로 받은 경기에만 있고 그 이전 경기는 " +
                "**영구히** 없다(LCU 히스토리에 타임라인이 없어 백필이 불가능하다). 404 로 답하면 대부분의 경기 상세 화면이 에러로 뜬다. 경기 자체가 없을 때만 404 다.",
    )
    val hasTimeline: Boolean,
    val durationMs: Long,
    @field:Schema(description = "프레임이 있는 마지막 분. 곡선의 x축 끝이다.")
    val lastMinute: Int,
    val teams: List<TeamTimelineSeriesResponse>,
    @field:Schema(description = "블루 − 레드. index = 분.")
    val teamGoldDiffByMinute: List<Int>,
    val participants: List<ParticipantTimelineSeriesResponse>,
    val kills: List<KillEntryResponse>,
    val objectives: List<ObjectiveEntryResponse>,
    val teamFights: List<TeamFightEntryResponse>,
) {
    companion object {
        fun from(result: MatchTimelineResult) =
            MatchTimelineResponse(
                matchId = result.matchId,
                hasTimeline = result.hasTimeline,
                durationMs = result.durationMs,
                lastMinute = result.lastMinute,
                teams = result.teams.map(TeamTimelineSeriesResponse::from),
                teamGoldDiffByMinute = result.teamGoldDiffByMinute,
                participants = result.participants.map(ParticipantTimelineSeriesResponse::from),
                kills = result.kills.map(KillEntryResponse::from),
                objectives = result.objectives.map(ObjectiveEntryResponse::from),
                teamFights = result.teamFights.map(TeamFightEntryResponse::from),
            )
    }
}

@Schema(name = "MapPointDto")
data class MapPointResponse(
    val x: Int,
    val y: Int,
) {
    companion object {
        fun from(result: MapPointDto) =
            MapPointResponse(
                x = result.x,
                y = result.y,
            )
    }
}

@Schema(name = "TeamTimelineSeries")
data class TeamTimelineSeriesResponse(
    val teamId: Int,
    val win: Boolean,
    @field:Schema(description = "팀 다섯 명의 합. index = 분.")
    val goldByMinute: List<Int>,
    val xpByMinute: List<Int>,
    val csByMinute: List<Int>,
    @field:Schema(description = "팀이 얼마나 뭉쳐 다녔나. 작을수록 붙어 있다. 자기 기지 안의 프레임은 빠진다.")
    val spreadByMinute: List<Int?>,
    val avgSpread: Int?,
) {
    companion object {
        fun from(result: TeamTimelineSeries) =
            TeamTimelineSeriesResponse(
                teamId = result.teamId,
                win = result.win,
                goldByMinute = result.goldByMinute,
                xpByMinute = result.xpByMinute,
                csByMinute = result.csByMinute,
                spreadByMinute = result.spreadByMinute,
                avgSpread = result.avgSpread,
            )
    }
}

@Schema(name = "ParticipantTimelineSeries")
data class ParticipantTimelineSeriesResponse(
    @field:Schema(description = "타임라인 프레임의 키(1~10). 프레임과 사람을 잇는 유일한 고리다.")
    val participantId: Int,
    val riotId: String,
    val champion: String,
    val championId: Int,
    val teamId: Int,
    val position: String,
    val win: Boolean,
    val goldByMinute: List<Int>,
    val xpByMinute: List<Int>,
    val csByMinute: List<Int>,
    val levelByMinute: List<Int>,
    @field:Schema(description = "손에 든 골드. 뚝 떨어지는 분이 아이템을 산 시점이다.")
    val currentGoldByMinute: List<Int>,
    @field:Schema(description = "같은 자리 상대와의 격차. 라인 상대가 없었으면 빈 목록.")
    val goldDiffByMinute: List<Int>,
    @field:Schema(description = "index = 분. null 은 그 프레임에 좌표가 없었다는 뜻이다.")
    val positionsByMinute: List<MapPointResponse?>,
    @field:Schema(description = "라인전 동안 자기 라인에 있던 비율. 정글·포지션 미상은 null.")
    val laneShareRate: Double?,
    @field:Schema(description = "라인전 동안 자기 라인도 자기 기지도 아닌 곳에 있던 비율. 정글은 null.")
    val roamRate: Double?,
    val enemyHalfRate: Double,
    val counterJungleRate: Double,
    @field:Schema(description = "위 비율들의 분모. 좌표가 있는 프레임 수(0분 제외)다.")
    val framesSampled: Int,
    val goldDiff15: Int?,
    val csDiff15: Int?,
    val xpDiff15: Int?,
    val earlyKills: Int,
    val earlyDeaths: Int,
    val earlyAssists: Int,
    val soloKills: Int,
    val firstDeathMs: Long?,
) {
    companion object {
        fun from(result: ParticipantTimelineSeries) =
            ParticipantTimelineSeriesResponse(
                participantId = result.participantId,
                riotId = result.riotId,
                champion = result.champion,
                championId = result.championId,
                teamId = result.teamId,
                position = result.position,
                win = result.win,
                goldByMinute = result.goldByMinute,
                xpByMinute = result.xpByMinute,
                csByMinute = result.csByMinute,
                levelByMinute = result.levelByMinute,
                currentGoldByMinute = result.currentGoldByMinute,
                goldDiffByMinute = result.goldDiffByMinute,
                positionsByMinute = result.positionsByMinute.map { it?.let(MapPointResponse::from) },
                laneShareRate = result.laneShareRate,
                roamRate = result.roamRate,
                enemyHalfRate = result.enemyHalfRate,
                counterJungleRate = result.counterJungleRate,
                framesSampled = result.framesSampled,
                goldDiff15 = result.goldDiff15,
                csDiff15 = result.csDiff15,
                xpDiff15 = result.xpDiff15,
                earlyKills = result.earlyKills,
                earlyDeaths = result.earlyDeaths,
                earlyAssists = result.earlyAssists,
                soloKills = result.soloKills,
                firstDeathMs = result.firstDeathMs,
            )
    }
}

@Schema(name = "KillEntry")
data class KillEntryResponse(
    val timestampMs: Long,
    val minute: Int,
    @field:Schema(description = "0이면 사람이 아니다 — 포탑이나 미니언이 막타를 쳤다.")
    val killerParticipantId: Int,
    val victimParticipantId: Int,
    val assistParticipantIds: List<Int>,
    @field:Schema(description = "킬을 올린 팀. 희생자의 팀을 뒤집어 구한다(막타가 포탑일 수 있어 킬러로는 못 센다).")
    val killingTeamId: Int?,
    val at: MapPointResponse?,
    @field:Schema(description = "[com.gijun.main.domain.match.service.MapRegion] 이름. 좌표가 없으면 null.")
    val region: String?,
) {
    companion object {
        fun from(result: KillEntry) =
            KillEntryResponse(
                timestampMs = result.timestampMs,
                minute = result.minute,
                killerParticipantId = result.killerParticipantId,
                victimParticipantId = result.victimParticipantId,
                assistParticipantIds = result.assistParticipantIds,
                killingTeamId = result.killingTeamId,
                at = result.at?.let(MapPointResponse::from),
                region = result.region,
            )
    }
}

@Schema(name = "ObjectiveEntry")
data class ObjectiveEntryResponse(
    val timestampMs: Long,
    val minute: Int,
    @field:Schema(description = "DRAGON, BARON_NASHOR, RIFTHERALD, HORDE, TOWER_BUILDING, INHIBITOR_BUILDING …")
    val kind: String,
    @field:Schema(description = "드래곤 원소(FIRE_DRAGON 등). 해당 없으면 빈 값.")
    val subType: String,
    @field:Schema(description = "포탑의 라인(TOP_LANE 등). 해당 없으면 빈 값.")
    val lane: String,
    @field:Schema(description = "OUTER_TURRET, INNER_TURRET, BASE_TURRET, NEXUS_TURRET. 억제기·몬스터는 빈 값.")
    val towerType: String,
    val killingTeamId: Int?,
    val killerParticipantId: Int,
    val assistParticipantIds: List<Int>,
    val at: MapPointResponse?,
) {
    companion object {
        fun from(result: ObjectiveEntry) =
            ObjectiveEntryResponse(
                timestampMs = result.timestampMs,
                minute = result.minute,
                kind = result.kind,
                subType = result.subType,
                lane = result.lane,
                towerType = result.towerType,
                killingTeamId = result.killingTeamId,
                killerParticipantId = result.killerParticipantId,
                assistParticipantIds = result.assistParticipantIds,
                at = result.at?.let(MapPointResponse::from),
            )
    }
}

@Schema(name = "TeamFightEntry")
data class TeamFightEntryResponse(
    val startMs: Long,
    val endMs: Long,
    val startMinute: Int,
    val team100Kills: Int,
    val team200Kills: Int,
    @field:Schema(description = "킬을 더 많이 올린 팀. 동수면 null — 교환으로 끝난 교전이다.")
    val winnerTeamId: Int?,
    val openedByTeamId: Int?,
    val participantIds: List<Int>,
    val at: MapPointResponse?,
    val region: String?,
    @get:JsonProperty("isTeamFight")
    val isTeamFight: Boolean,
    @field:Schema(description = "교전 직후 넘어간 오브젝트의 종류. 전리품이다.")
    val objectiveKinds: List<String>,
) {
    companion object {
        fun from(result: TeamFightEntry) =
            TeamFightEntryResponse(
                startMs = result.startMs,
                endMs = result.endMs,
                startMinute = result.startMinute,
                team100Kills = result.team100Kills,
                team200Kills = result.team200Kills,
                winnerTeamId = result.winnerTeamId,
                openedByTeamId = result.openedByTeamId,
                participantIds = result.participantIds,
                at = result.at?.let(MapPointResponse::from),
                region = result.region,
                isTeamFight = result.isTeamFight,
                objectiveKinds = result.objectiveKinds,
            )
    }
}
