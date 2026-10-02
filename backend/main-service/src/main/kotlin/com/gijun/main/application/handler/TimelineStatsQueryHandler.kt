package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetTimelineChampionsQuery
import com.gijun.main.application.dto.query.GetTimelineLaneQuery
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
import com.gijun.main.application.port.`in`.GetPlayerTimelineUseCase
import com.gijun.main.application.port.`in`.GetTimelineChampionsUseCase
import com.gijun.main.application.port.`in`.GetTimelineLaneUseCase
import com.gijun.main.application.port.`in`.GetTimelineStatsUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.HEATMAP_GRID
import com.gijun.main.application.port.out.persistence.HeatmapKind
import com.gijun.main.application.port.out.persistence.HeatmapScope
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.StatsCacheQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.match.service.TimelineMetrics
import com.gijun.main.domain.match.service.TimelineParser
import com.gijun.main.shared.domain.vo.RiotId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToInt

@Service
@Transactional(readOnly = true)
class TimelineStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
    private val statsCacheQueryPersistencePort: StatsCacheQueryPersistencePort,
) : GetTimelineStatsUseCase,
    GetTimelineLaneUseCase,
    GetTimelineChampionsUseCase,
    GetPlayerTimelineUseCase {
    /**
     * 타임라인 기반 지표. 모든 화면이 같은 경기 단위 계산([TimelineMetrics])과 같은 평균([averages])을 공유한다.
     *
     * 타임라인은 새 수집기로 받은 경기에만 있다. 여기 나오는 모든 수치의 모집단은
     * "전체 경기"가 아니라 "타임라인이 있는 경기"다 — 결과에 경기 수를 빠짐없이 싣는 이유다.
     */
    override fun getTimelineStats(mode: GameMode): TimelineStatsResult =
        statsResultCacheQueryPort.getOrCompute("timeline-stats:${mode.key}") {
            val metrics = metrics(mode)
            val lines = lines(metrics)
            val decided = metrics.filter { it.goldLeadTeamAt15 != null && it.winnerTeamId != null }

            TimelineStatsResult(
                games = metrics.size,
                goldLeadWinRate =
                    decided
                        .takeIf { it.isNotEmpty() }
                        ?.let { d -> r1(d.count { it.goldLeadTeamAt15 == it.winnerTeamId } * 100.0 / d.size) },
                goldLeadGames = decided.size,
                comebackGames = decided.count { it.goldLeadTeamAt15 != it.winnerTeamId && it.teamGoldGapAt15 >= COMEBACK_GAP },
                avgTeamGoldGapAt15 = decided.takeIf { it.isNotEmpty() }?.let { d -> r1(d.map { it.teamGoldGapAt15 }.average()) } ?: 0.0,
                players = players(lines) { mainPosition(it) },
                positions = byPosition(lines, order = POSITION_ORDER),
            )
        }

    override fun getTimelineLane(query: GetTimelineLaneQuery): TimelineLaneResult {
        val position = query.lane.uppercase()
        return statsResultCacheQueryPort.getOrCompute("timeline-lane:$position:${query.mode.key}") {
            val lines = lines(metrics(query.mode)).filter { it.line.position.equals(position, ignoreCase = true) }
            TimelineLaneResult(
                position = position,
                summary = lines.takeIf { it.isNotEmpty() }?.let(::averages),
                players = players(lines) { position },
            )
        }
    }

    override fun getTimelineChampions(query: GetTimelineChampionsQuery): TimelineChampionsResult {
        val all =
            statsResultCacheQueryPort.getOrCompute("timeline-champions:${query.mode.key}") {
                val metrics = metrics(query.mode)
                TimelineChampionsResult(games = metrics.size, champions = champions(lines(metrics)))
            }
        if (query.champion.isNullOrBlank()) return all

        val name = query.champion.trim()
        return statsResultCacheQueryPort.getOrCompute("timeline-champion-detail:$name:${query.mode.key}") {
            val mine = lines(metrics(query.mode)).filter { it.line.champion.equals(name, ignoreCase = true) }
            all.copy(
                champions = all.champions.filter { it.champion.equals(name, ignoreCase = true) },
                curve = curve(mine),
                // 표본이 적을 때 평균을 억지로 내지 않고 판을 그대로 보여 주기 위한 목록이다.
                matches =
                    mine.map { l ->
                        TimelineChampionGame(
                            matchId = l.matchId,
                            gameCreation = l.gameCreation,
                            riotId = l.riotId,
                            position = l.line.position,
                            win = l.line.win,
                            goldDiff15 = l.line.goldDiff15,
                            csDiff15 = l.line.csDiff15,
                            opponentChampion = l.line.opponentChampion,
                            opponentChampionId = l.line.opponentChampionId,
                            goldDiffByMinute = l.line.goldDiffByMinute,
                        )
                    },
            )
        }
    }

    /**
     * 개인 타임라인. 사람마다 전체 경기의 라인을 다시 묶으므로 자기 캐시 키가 필요하다.
     *
     * 안쪽에서 [metrics] / [getTimelineStats] 의 `getOrCompute` 를 다시 부르는데, 중첩은 안전하다 —
     * 키가 다르면 다른 스트라이프 락이고, 해시가 같은 스트라이프에 떨어져도 `synchronized` 는
     * 같은 스레드에 재진입 가능하다. (데드락으로 의심하기 쉬운 자리라 남겨 둔다.)
     */
    override fun getPlayerTimeline(riotId: RiotId): PlayerTimelineResult =
        statsResultCacheQueryPort.getOrCompute("player-timeline:${riotId.value}") {
            val id = riotId.value
            // 개인 화면은 모드 구분이 없다 (칼바람은 라인이 없어 어차피 격차가 안 나온다).
            val mine = lines(metrics(MODE)).filter { it.riotId == id }
            val snapshot = statsCacheQueryPersistencePort.findPlayerTimelineCache(RiotId(id), MODE)

            val ranked = getTimelineStats(MODE).players.filter { it.stats.avgGoldDiff15 != null }
            val rank = ranked.indexOfFirst { it.riotId == id }.takeIf { it >= 0 }?.plus(1)

            PlayerTimelineResult(
                riotId = id,
                summary = mine.takeIf { it.isNotEmpty() }?.let(::averages),
                goldDiffRank = rank,
                rankedPlayers = ranked.size,
                byPosition = byPosition(mine, order = null),
                byChampion = champions(mine),
                goldDiffCurve = curve(mine),
                positionStats =
                    snapshot?.let { c ->
                        PlayerPositionStats(
                            games = c.games,
                            framesSampled = c.framesSampled,
                            laneShareRate = c.laneShareRate,
                            roamRate = c.roamRate,
                            enemyHalfRate = c.enemyHalfRate,
                            counterJungleRate = c.counterJungleRate,
                            teamfights = c.teamfights,
                            teamfightKills = c.teamfightKills,
                            teamfightDeaths = c.teamfightDeaths,
                            aggregatedAt = c.aggregatedAt,
                        )
                    },
                deathHeatmap = heatmap(HeatmapScope.PLAYER.name, id, HeatmapKind.DEATH.name, snapshot?.aggregatedAt),
                games =
                    mine.take(RECENT_GAMES).map { l ->
                        PlayerTimelineGame(
                            matchId = l.matchId,
                            gameCreation = l.gameCreation,
                            champion = l.line.champion,
                            championId = l.line.championId,
                            position = l.line.position,
                            win = l.line.win,
                            opponentRiotId = l.line.opponentRiotId,
                            opponentChampion = l.line.opponentChampion,
                            opponentChampionId = l.line.opponentChampionId,
                            goldDiff15 = l.line.goldDiff15,
                            csDiff15 = l.line.csDiff15,
                            xpDiff15 = l.line.xpDiff15,
                            earlyKills = l.line.earlyKills,
                            earlyDeaths = l.line.earlyDeaths,
                            earlyAssists = l.line.earlyAssists,
                            soloKills = l.line.soloKills,
                            goldDiffByMinute = l.line.goldDiffByMinute,
                        )
                    },
            )
        }

    // ────────── 묶기 ──────────

    /** 한 사람의 한 경기. riotId 는 정규화된 이름이다. */
    private class Line(
        val riotId: String,
        val matchId: String,
        val gameCreation: Long,
        val line: TimelineMetrics.PlayerLine,
    )

    /**
     * 최신순. 원본 파싱이 무거워서 경기 단위 결과를 따로 캐시한다.
     *
     * 청크로 나눠 읽는 이유는 힙이다. 원본을 한 번에 들어올리면 raw 문자열 전체와 파싱 중인
     * JsonNode 트리가 같은 시점에 살아 있다. 청크 안에서 [raws] 가 바로 죽으므로
     * 살아남는 건 [TimelineMetrics.MatchMetrics] 뿐이다.
     */
    private fun metrics(mode: GameMode): List<TimelineMetrics.MatchMetrics> =
        statsResultCacheQueryPort.getOrCompute("timeline-metrics:${mode.key}") {
            matchQueryPersistencePort
                .findAllWithParticipants(mode.queueIds)
                .chunked(TimelineParser.CHUNK_SIZE)
                .flatMap { chunk ->
                    val raws = matchQueryPersistencePort.findTimelineRaw(chunk.map { it.matchId })
                    chunk.mapNotNull { m -> raws[m.matchId]?.let { TimelineMetrics.of(m, TimelineParser.parse(it)) } }
                }.sortedByDescending { it.gameCreation }
        }

    private fun lines(metrics: List<TimelineMetrics.MatchMetrics>): List<Line> =
        metrics.flatMap { m ->
            m.players.map { Line(it.riotId, m.matchId, m.gameCreation, it) }
        }

    /** 15분 골드 격차 순. 격차를 못 잰 사람(라인 상대가 없던 사람)은 뒤로. */
    private fun players(
        lines: List<Line>,
        position: (List<Line>) -> String?,
    ): List<TimelinePlayerEntry> =
        lines
            .groupBy { it.riotId }
            .map { (riotId, ls) -> TimelinePlayerEntry(riotId, position(ls), averages(ls)) }
            .sortedWith(
                compareByDescending<TimelinePlayerEntry> { it.stats.avgGoldDiff15 != null }
                    .thenByDescending { it.stats.avgGoldDiff15 ?: 0.0 }
                    .thenByDescending { it.stats.games },
            )

    /** [order] 가 있으면 그 순서(라인 순), 없으면 경기 수 내림차순. 포지션이 비었거나 UNKNOWN 인 경기는 뺀다. */
    private fun byPosition(
        lines: List<Line>,
        order: List<String>?,
    ): List<TimelinePositionEntry> {
        val entries =
            lines
                .filter { it.line.position.uppercase() in POSITION_ORDER }
                .groupBy { it.line.position.uppercase() }
                .map { (position, ls) -> TimelinePositionEntry(position, averages(ls)) }
        return if (order != null) {
            entries.sortedBy { order.indexOf(it.position).let { i -> if (i < 0) order.size else i } }
        } else {
            entries.sortedByDescending { it.stats.games }
        }
    }

    private fun champions(lines: List<Line>): List<TimelineChampionEntry> =
        lines
            .groupBy { it.line.champion }
            .map { (champion, ls) ->
                TimelineChampionEntry(
                    champion = champion,
                    championId = ls.first().line.championId,
                    stats = averages(ls),
                    byPosition = byPosition(ls, order = null),
                )
            }.sortedByDescending { it.stats.games }

    /**
     * 분별 평균 골드 격차.
     *
     * 뒤로 갈수록 그 시간까지 간 경기가 줄어든다 — 점마다 표본 수를 같이 실어서 화면이
     * "30분 곡선이 1경기짜리"라는 걸 숨기지 않게 한다.
     */
    private fun curve(lines: List<Line>): List<GoldDiffPoint> =
        (0..TimelineMetrics.CURVE_MAX_MINUTE).mapNotNull { minute ->
            val values = lines.mapNotNull { it.line.goldDiffByMinute.getOrNull(minute) }
            if (values.isEmpty()) null else GoldDiffPoint(minute, r1(values.average()), values.size)
        }

    private fun mainPosition(lines: List<Line>): String? =
        lines
            .map { it.line.position }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    private fun averages(lines: List<Line>): TimelineAverages {
        val ls = lines.map { it.line }
        val games = ls.size
        val laneDiffs = ls.mapNotNull { it.goldDiff15 }
        val led = ls.filter { (it.goldDiff15 ?: 0) > 0 }

        return TimelineAverages(
            games = games,
            laneGames = laneDiffs.size,
            winRate = r1(ls.count { it.win } * 100.0 / games),
            avgGoldDiff15 = avgOrNull(laneDiffs),
            avgCsDiff15 = avgOrNull(ls.mapNotNull { it.csDiff15 }),
            avgXpDiff15 = avgOrNull(ls.mapNotNull { it.xpDiff15 }),
            laneLeadRate = laneDiffs.takeIf { it.isNotEmpty() }?.let { d -> r1(d.count { it > 0 } * 100.0 / d.size) },
            leadWinRate = led.takeIf { it.isNotEmpty() }?.let { d -> r1(d.count { it.win } * 100.0 / d.size) },
            leadGames = led.size,
            avgCsAt10 = avgOrNull(ls.mapNotNull { it.csAt10 }),
            avgGoldAt15 = avgOrNull(ls.mapNotNull { it.goldAt15 }),
            avgEarlyKills = r1(ls.map { it.earlyKills }.average()),
            avgEarlyDeaths = r1(ls.map { it.earlyDeaths }.average()),
            avgEarlyAssists = r1(ls.map { it.earlyAssists }.average()),
            avgSoloKills = r1(ls.map { it.soloKills }.average()),
            firstBloodRate = r1(ls.count { it.firstBlood } * 100.0 / games),
            avgFirstDeathMinute =
                ls
                    .mapNotNull { it.firstDeathMs }
                    .takeIf { it.isNotEmpty() }
                    ?.let { d -> r1(d.average() / 60_000.0) },
        )
    }

    /**
     * 히트맵. **live fallback 이 없다** — 격자 집계는 프레임·이벤트 전수를 순회해야 나오므로
     * 요청 시 돌릴 비용이 아니다. 배치 전이면 `aggregatedAt = null` 로 내려보내 화면이
     * "집계 대기 중"과 "정말 데이터가 없음"을 구분할 수 있게 한다.
     */
    private fun heatmap(
        scopeType: String,
        scopeKey: String,
        kind: String,
        aggregatedAt: Long?,
    ): HeatmapGrid =
        HeatmapGrid(
            grid = HEATMAP_GRID,
            cells =
                statsCacheQueryPersistencePort
                    .findHeatmap(MODE, scopeType, scopeKey, kind)
                    .map { HeatmapCellEntry(it.phase, it.gridX, it.gridY, it.count) },
            // 히트맵과 좌표 지표는 같은 배치가 같은 시점에 쓴다. 그 시각이 없으면 아직 안 돌았다.
            aggregatedAt = aggregatedAt,
        )

    private fun avgOrNull(values: List<Int>): Double? = values.takeIf { it.isNotEmpty() }?.let { r1(it.average()) }

    private fun r1(v: Double) = (v * 10).roundToInt() / 10.0

    private companion object {
        val MODE = GameMode.ALL
        const val RECENT_GAMES = 20

        /** 이만큼 뒤진 걸 뒤집어야 역전승으로 친다. 1,000 이하는 킬 하나로 오가는 폭이다. */
        const val COMEBACK_GAP = 1_500

        /** 라인 순서. UNKNOWN 은 포지션 추정이 실패한 참가자라 어느 표에도 싣지 않는다. */
        val POSITION_ORDER = Position.entries.filter { it != Position.UNKNOWN }.map { it.name }
    }
}
