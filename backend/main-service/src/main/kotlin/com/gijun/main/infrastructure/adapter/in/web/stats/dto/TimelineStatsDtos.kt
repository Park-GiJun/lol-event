package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.GoldDiffPoint
import com.gijun.main.application.dto.result.HeatmapCellEntry
import com.gijun.main.application.dto.result.HeatmapGrid
import com.gijun.main.application.dto.result.PlayerPositionStats
import com.gijun.main.application.dto.result.PlayerTimelineGame
import com.gijun.main.application.dto.result.PlayerTimelineResult
import com.gijun.main.application.dto.result.TimelineAverages
import com.gijun.main.application.dto.result.TimelineChampionEntry
import com.gijun.main.application.dto.result.TimelineChampionGame
import com.gijun.main.application.dto.result.TimelineChampionsResult
import com.gijun.main.application.dto.result.TimelineLaneResult
import com.gijun.main.application.dto.result.TimelinePlayerEntry
import com.gijun.main.application.dto.result.TimelinePositionEntry
import com.gijun.main.application.dto.result.TimelineStatsResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "TimelineAverages")
data class TimelineAveragesResponse(
    @field:Schema(description = "타임라인이 있는 경기 수.")
    val games: Int,
    @field:Schema(description = "그중 라인 상대가 있어 격차를 잴 수 있었던 경기 수.")
    val laneGames: Int,
    val winRate: Double,
    @field:Schema(description = "격차는 전부 \"나 − 같은 자리 상대\"다.")
    val avgGoldDiff15: Double?,
    val avgCsDiff15: Double?,
    val avgXpDiff15: Double?,
    @field:Schema(description = "15분 골드가 라인 상대보다 앞선 경기 비율.")
    val laneLeadRate: Double?,
    @field:Schema(description = "15분에 라인 상대보다 앞섰던 경기의 승률. \"라인을 이기면 게임도 이기나\".")
    val leadWinRate: Double?,
    val leadGames: Int,
    val avgCsAt10: Double?,
    val avgGoldAt15: Double?,
    val avgEarlyKills: Double,
    val avgEarlyDeaths: Double,
    val avgEarlyAssists: Double,
    val avgSoloKills: Double,
    val firstBloodRate: Double,
    @field:Schema(description = "죽은 경기만 평균낸다. 한 번도 안 죽었으면 null.")
    val avgFirstDeathMinute: Double?,
) {
    companion object {
        fun from(result: TimelineAverages) =
            TimelineAveragesResponse(
                games = result.games,
                laneGames = result.laneGames,
                winRate = result.winRate,
                avgGoldDiff15 = result.avgGoldDiff15,
                avgCsDiff15 = result.avgCsDiff15,
                avgXpDiff15 = result.avgXpDiff15,
                laneLeadRate = result.laneLeadRate,
                leadWinRate = result.leadWinRate,
                leadGames = result.leadGames,
                avgCsAt10 = result.avgCsAt10,
                avgGoldAt15 = result.avgGoldAt15,
                avgEarlyKills = result.avgEarlyKills,
                avgEarlyDeaths = result.avgEarlyDeaths,
                avgEarlyAssists = result.avgEarlyAssists,
                avgSoloKills = result.avgSoloKills,
                firstBloodRate = result.firstBloodRate,
                avgFirstDeathMinute = result.avgFirstDeathMinute,
            )
    }
}

@Schema(name = "TimelinePlayerEntry")
data class TimelinePlayerEntryResponse(
    val riotId: String,
    @field:Schema(description = "전체 표에서는 가장 많이 선 포지션, 라인별 표에서는 그 라인.")
    val position: String?,
    val stats: TimelineAveragesResponse,
) {
    companion object {
        fun from(result: TimelinePlayerEntry) =
            TimelinePlayerEntryResponse(
                riotId = result.riotId,
                position = result.position,
                stats = TimelineAveragesResponse.from(result.stats),
            )
    }
}

@Schema(name = "TimelinePositionEntry")
data class TimelinePositionEntryResponse(
    val position: String,
    val stats: TimelineAveragesResponse,
) {
    companion object {
        fun from(result: TimelinePositionEntry) =
            TimelinePositionEntryResponse(
                position = result.position,
                stats = TimelineAveragesResponse.from(result.stats),
            )
    }
}

@Schema(name = "TimelineChampionEntry")
data class TimelineChampionEntryResponse(
    val champion: String,
    val championId: Int,
    val stats: TimelineAveragesResponse,
    @field:Schema(description = "포지션별로 쪼갠 것. 경기 수 내림차순.")
    val byPosition: List<TimelinePositionEntryResponse>,
) {
    companion object {
        fun from(result: TimelineChampionEntry) =
            TimelineChampionEntryResponse(
                champion = result.champion,
                championId = result.championId,
                stats = TimelineAveragesResponse.from(result.stats),
                byPosition = result.byPosition.map(TimelinePositionEntryResponse::from),
            )
    }
}

@Schema(name = "TimelineStatsResult")
data class TimelineStatsResponse(
    @field:Schema(description = "타임라인이 있는 경기 수.")
    val games: Int,
    @field:Schema(description = "15분 골드가 앞선 팀의 승률. 판정 가능한 경기가 없으면 null.")
    val goldLeadWinRate: Double?,
    val goldLeadGames: Int,
    @field:Schema(description = "15분에 1,500골드 이상 뒤지고도 이긴 경기 수.")
    val comebackGames: Int,
    val avgTeamGoldGapAt15: Double,
    val players: List<TimelinePlayerEntryResponse>,
    @field:Schema(description = "TOP → SUPPORT 순. 라인 평균이라 격차는 0 근처다 — 절댓값 지표를 본다.")
    val positions: List<TimelinePositionEntryResponse>,
) {
    companion object {
        fun from(result: TimelineStatsResult) =
            TimelineStatsResponse(
                games = result.games,
                goldLeadWinRate = result.goldLeadWinRate,
                goldLeadGames = result.goldLeadGames,
                comebackGames = result.comebackGames,
                avgTeamGoldGapAt15 = result.avgTeamGoldGapAt15,
                players = result.players.map(TimelinePlayerEntryResponse::from),
                positions = result.positions.map(TimelinePositionEntryResponse::from),
            )
    }
}

@Schema(name = "TimelineLaneResult")
data class TimelineLaneResponse(
    val position: String,
    @field:Schema(description = "그 라인 전체 평균. 경기가 없으면 null.")
    val summary: TimelineAveragesResponse?,
    @field:Schema(description = "그 라인에서 뛴 경기만 센 선수별 평균.")
    val players: List<TimelinePlayerEntryResponse>,
) {
    companion object {
        fun from(result: TimelineLaneResult) =
            TimelineLaneResponse(
                position = result.position,
                summary = result.summary?.let(TimelineAveragesResponse::from),
                players = result.players.map(TimelinePlayerEntryResponse::from),
            )
    }
}

@Schema(name = "TimelineChampionsResult")
data class TimelineChampionsResponse(
    val games: Int,
    @field:Schema(description = "경기 수 내림차순.")
    val champions: List<TimelineChampionEntryResponse>,
    @field:Schema(description = "분별 평균 골드 격차. **`champion` 을 지정해 부를 때만 채운다** — 챔피언마다 곡선을 계산해 전부 실으면 목록 응답이 쓸데없이 커진다.")
    val curve: List<GoldDiffPointResponse>,
    @field:Schema(description = "그 챔피언이 나온 경기. 최신순. 위와 같은 이유로 `champion` 지정 시에만 채운다.")
    val matches: List<TimelineChampionGameResponse>,
) {
    companion object {
        fun from(result: TimelineChampionsResult) =
            TimelineChampionsResponse(
                games = result.games,
                champions = result.champions.map(TimelineChampionEntryResponse::from),
                curve = result.curve.map(GoldDiffPointResponse::from),
                matches = result.matches.map(TimelineChampionGameResponse::from),
            )
    }
}

@Schema(name = "TimelineChampionGame")
data class TimelineChampionGameResponse(
    val matchId: String,
    val gameCreation: Long,
    val riotId: String,
    val position: String,
    val win: Boolean,
    val goldDiff15: Int?,
    val csDiff15: Int?,
    val opponentChampion: String?,
    val opponentChampionId: Int?,
    @field:Schema(description = "index = 분. 라인 상대가 없었으면 빈 목록.")
    val goldDiffByMinute: List<Int>,
) {
    companion object {
        fun from(result: TimelineChampionGame) =
            TimelineChampionGameResponse(
                matchId = result.matchId,
                gameCreation = result.gameCreation,
                riotId = result.riotId,
                position = result.position,
                win = result.win,
                goldDiff15 = result.goldDiff15,
                csDiff15 = result.csDiff15,
                opponentChampion = result.opponentChampion,
                opponentChampionId = result.opponentChampionId,
                goldDiffByMinute = result.goldDiffByMinute,
            )
    }
}

@Schema(name = "PlayerTimelineResult")
data class PlayerTimelineResponse(
    val riotId: String,
    @field:Schema(description = "타임라인 경기가 없으면 null.")
    val summary: TimelineAveragesResponse?,
    @field:Schema(description = "15분 골드 격차 순위. 라인 경기가 없으면 null.")
    val goldDiffRank: Int?,
    val rankedPlayers: Int,
    val byPosition: List<TimelinePositionEntryResponse>,
    val byChampion: List<TimelineChampionEntryResponse>,
    @field:Schema(description = "분별 평균 골드 격차.")
    val goldDiffCurve: List<GoldDiffPointResponse>,
    @field:Schema(description = "최신순.")
    val games: List<PlayerTimelineGameResponse>,
    @field:Schema(
        description =
            "좌표·한타 지표. 배치가 아직 안 돌았으면 null 이다. 15분 격차 계열과 달리 요청 시 계산하지 않는다 — 프레임 전수를 순회해야 나오는 값이라 매 요청에 돌릴 비용이 " +
                "아니다.",
    )
    val positionStats: PlayerPositionStatsResponse?,
    @field:Schema(description = "죽은 자리 히트맵. 배치 전이면 빈 목록이고, 화면은 \"집계 대기 중\"을 띄운다.")
    val deathHeatmap: HeatmapGridResponse?,
) {
    companion object {
        fun from(result: PlayerTimelineResult) =
            PlayerTimelineResponse(
                riotId = result.riotId,
                summary = result.summary?.let(TimelineAveragesResponse::from),
                goldDiffRank = result.goldDiffRank,
                rankedPlayers = result.rankedPlayers,
                byPosition = result.byPosition.map(TimelinePositionEntryResponse::from),
                byChampion = result.byChampion.map(TimelineChampionEntryResponse::from),
                goldDiffCurve = result.goldDiffCurve.map(GoldDiffPointResponse::from),
                games = result.games.map(PlayerTimelineGameResponse::from),
                positionStats = result.positionStats?.let(PlayerPositionStatsResponse::from),
                deathHeatmap = result.deathHeatmap?.let(HeatmapGridResponse::from),
            )
    }
}

@Schema(name = "PlayerPositionStats")
data class PlayerPositionStatsResponse(
    @field:Schema(description = "이 지표들의 모집단. 타임라인이 있는 경기 수다.")
    val games: Int,
    val framesSampled: Int,
    @field:Schema(description = "라인전 동안 자기 라인에 있던 비율. **정글·포지션 미상은 null** (0 이 아니다).")
    val laneShareRate: Double?,
    val roamRate: Double?,
    val enemyHalfRate: Double,
    val counterJungleRate: Double,
    @field:Schema(description = "킬 3개 이상인 교전에 낀 횟수.")
    val teamfights: Int,
    val teamfightKills: Int,
    val teamfightDeaths: Int,
    @field:Schema(description = "이 값이 집계된 시각. 실시간 계산이 아니라는 표시이기도 하다.")
    val aggregatedAt: Long,
) {
    companion object {
        fun from(result: PlayerPositionStats) =
            PlayerPositionStatsResponse(
                games = result.games,
                framesSampled = result.framesSampled,
                laneShareRate = result.laneShareRate,
                roamRate = result.roamRate,
                enemyHalfRate = result.enemyHalfRate,
                counterJungleRate = result.counterJungleRate,
                teamfights = result.teamfights,
                teamfightKills = result.teamfightKills,
                teamfightDeaths = result.teamfightDeaths,
                aggregatedAt = result.aggregatedAt,
            )
    }
}

@Schema(name = "HeatmapGrid")
data class HeatmapGridResponse(
    @field:Schema(description = "한 변의 칸 수. 좌표를 비율로 바꿀 때 쓴다.")
    val grid: Int,
    val cells: List<HeatmapCellEntryResponse>,
    @field:Schema(description = "배치 전이면 null. [cells] 가 비어 있는 것과 구분해야 \"집계 대기 중\"을 띄울 수 있다.")
    val aggregatedAt: Long?,
) {
    companion object {
        fun from(result: HeatmapGrid) =
            HeatmapGridResponse(
                grid = result.grid,
                cells = result.cells.map(HeatmapCellEntryResponse::from),
                aggregatedAt = result.aggregatedAt,
            )
    }
}

@Schema(name = "HeatmapCellEntry")
data class HeatmapCellEntryResponse(
    @field:Schema(description = "EARLY(0~15분) / MID(15~25) / LATE(25+).")
    val phase: String,
    val x: Int,
    val y: Int,
    val count: Int,
) {
    companion object {
        fun from(result: HeatmapCellEntry) =
            HeatmapCellEntryResponse(
                phase = result.phase,
                x = result.x,
                y = result.y,
                count = result.count,
            )
    }
}

@Schema(name = "GoldDiffPoint")
data class GoldDiffPointResponse(
    val minute: Int,
    val avgGoldDiff: Double,
    val games: Int,
) {
    companion object {
        fun from(result: GoldDiffPoint) =
            GoldDiffPointResponse(
                minute = result.minute,
                avgGoldDiff = result.avgGoldDiff,
                games = result.games,
            )
    }
}

@Schema(name = "PlayerTimelineGame")
data class PlayerTimelineGameResponse(
    val matchId: String,
    val gameCreation: Long,
    val champion: String,
    val championId: Int,
    val position: String,
    val win: Boolean,
    val opponentRiotId: String?,
    val opponentChampion: String?,
    val opponentChampionId: Int?,
    val goldDiff15: Int?,
    val csDiff15: Int?,
    val xpDiff15: Int?,
    val earlyKills: Int,
    val earlyDeaths: Int,
    val earlyAssists: Int,
    val soloKills: Int,
    val goldDiffByMinute: List<Int>,
) {
    companion object {
        fun from(result: PlayerTimelineGame) =
            PlayerTimelineGameResponse(
                matchId = result.matchId,
                gameCreation = result.gameCreation,
                champion = result.champion,
                championId = result.championId,
                position = result.position,
                win = result.win,
                opponentRiotId = result.opponentRiotId,
                opponentChampion = result.opponentChampion,
                opponentChampionId = result.opponentChampionId,
                goldDiff15 = result.goldDiff15,
                csDiff15 = result.csDiff15,
                xpDiff15 = result.xpDiff15,
                earlyKills = result.earlyKills,
                earlyDeaths = result.earlyDeaths,
                earlyAssists = result.earlyAssists,
                soloKills = result.soloKills,
                goldDiffByMinute = result.goldDiffByMinute,
            )
    }
}
