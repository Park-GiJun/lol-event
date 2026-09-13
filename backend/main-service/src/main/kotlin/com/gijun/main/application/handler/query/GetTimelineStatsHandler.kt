package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.GoldDiffPoint
import com.gijun.main.application.dto.stats.result.PlayerTimelineGame
import com.gijun.main.application.dto.stats.result.PlayerTimelineResult
import com.gijun.main.application.dto.stats.result.TimelineAverages
import com.gijun.main.application.dto.stats.result.TimelineChampionEntry
import com.gijun.main.application.dto.stats.result.TimelineChampionsResult
import com.gijun.main.application.dto.stats.result.TimelineLaneResult
import com.gijun.main.application.dto.stats.result.TimelinePlayerEntry
import com.gijun.main.application.dto.stats.result.TimelinePositionEntry
import com.gijun.main.application.dto.stats.result.TimelineStatsResult
import com.gijun.main.application.port.`in`.GetPlayerTimelineUseCase
import com.gijun.main.application.port.`in`.GetTimelineStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.model.match.Position
import com.gijun.main.domain.service.RiotIdNormalizer
import com.gijun.main.domain.service.TimelineMetrics
import com.gijun.main.domain.service.TimelineParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToInt

/**
 * 타임라인 기반 지표. 모든 화면이 같은 경기 단위 계산([TimelineMetrics])과 같은 평균([averages])을 공유한다.
 *
 * 타임라인은 새 수집기로 받은 경기에만 있다. 여기 나오는 모든 수치의 모집단은
 * "전체 경기"가 아니라 "타임라인이 있는 경기"다 — 결과에 경기 수를 빠짐없이 싣는 이유다.
 *
 * riotId 는 [RiotIdNormalizer] 로 합쳐서 센다. 개인 화면은 레이팅과 같은 이름 체계를 쓰기 때문이다.
 */
@Service
@Transactional(readOnly = true)
class GetTimelineStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
    private val normalizer: RiotIdNormalizer,
) : GetTimelineStatsUseCase, GetPlayerTimelineUseCase {

    override fun getTimelineStats(mode: String): TimelineStatsResult = cache.getOrCompute("timeline-stats:$mode") {
        val metrics = metrics(mode)
        val lines = lines(metrics)
        val decided = metrics.filter { it.goldLeadTeamAt15 != null && it.winnerTeamId != null }

        TimelineStatsResult(
            games = metrics.size,
            goldLeadWinRate = decided.takeIf { it.isNotEmpty() }
                ?.let { d -> r1(d.count { it.goldLeadTeamAt15 == it.winnerTeamId } * 100.0 / d.size) },
            goldLeadGames = decided.size,
            comebackGames = decided.count { it.goldLeadTeamAt15 != it.winnerTeamId && it.teamGoldGapAt15 >= COMEBACK_GAP },
            avgTeamGoldGapAt15 = decided.takeIf { it.isNotEmpty() }?.let { d -> r1(d.map { it.teamGoldGapAt15 }.average()) } ?: 0.0,
            players = players(lines) { mainPosition(it) },
            positions = byPosition(lines, order = POSITION_ORDER),
        )
    }

    override fun getTimelineLane(lane: String, mode: String): TimelineLaneResult {
        val position = lane.uppercase()
        return cache.getOrCompute("timeline-lane:$position:$mode") {
            val lines = lines(metrics(mode)).filter { it.line.position.equals(position, ignoreCase = true) }
            TimelineLaneResult(
                position = position,
                summary = lines.takeIf { it.isNotEmpty() }?.let(::averages),
                players = players(lines) { position },
            )
        }
    }

    override fun getTimelineChampions(mode: String, champion: String?): TimelineChampionsResult {
        val all = cache.getOrCompute("timeline-champions:$mode") {
            val metrics = metrics(mode)
            TimelineChampionsResult(games = metrics.size, champions = champions(lines(metrics)))
        }
        if (champion.isNullOrBlank()) return all
        return all.copy(champions = all.champions.filter { it.champion.equals(champion.trim(), ignoreCase = true) })
    }

    override fun getPlayerTimeline(riotId: String): PlayerTimelineResult {
        val id = normalizer.canonical(riotId)
        // 개인 화면은 모드 구분이 없다 (칼바람은 라인이 없어 어차피 격차가 안 나온다).
        val mine = lines(metrics(MODE)).filter { it.riotId == id }

        val ranked = getTimelineStats(MODE).players.filter { it.stats.avgGoldDiff15 != null }
        val rank = ranked.indexOfFirst { it.riotId == id }.takeIf { it >= 0 }?.plus(1)

        val curve = (0..TimelineMetrics.CURVE_MAX_MINUTE).mapNotNull { minute ->
            val values = mine.mapNotNull { it.line.goldDiffByMinute.getOrNull(minute) }
            if (values.isEmpty()) null else GoldDiffPoint(minute, r1(values.average()), values.size)
        }

        return PlayerTimelineResult(
            riotId = id,
            summary = mine.takeIf { it.isNotEmpty() }?.let(::averages),
            goldDiffRank = rank,
            rankedPlayers = ranked.size,
            byPosition = byPosition(mine, order = null),
            byChampion = champions(mine),
            goldDiffCurve = curve,
            games = mine.take(RECENT_GAMES).map { l ->
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

    /** 최신순. 원본 파싱이 무거워서 경기 단위 결과를 따로 캐시한다. */
    private fun metrics(mode: String): List<TimelineMetrics.MatchMetrics> = cache.getOrCompute("timeline-metrics:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))
        val raws = matchPersistencePort.findTimelineRaw(matches.map { it.matchId })
        matches
            .mapNotNull { m -> raws[m.matchId]?.let { TimelineMetrics.of(m, TimelineParser.parse(it)) } }
            .sortedByDescending { it.gameCreation }
    }

    private fun lines(metrics: List<TimelineMetrics.MatchMetrics>): List<Line> =
        metrics.flatMap { m ->
            m.players.map { Line(normalizer.canonical(it.riotId), m.matchId, m.gameCreation, it) }
        }

    /** 15분 골드 격차 순. 격차를 못 잰 사람(라인 상대가 없던 사람)은 뒤로. */
    private fun players(lines: List<Line>, position: (List<Line>) -> String?): List<TimelinePlayerEntry> =
        lines.groupBy { it.riotId }
            .map { (riotId, ls) -> TimelinePlayerEntry(riotId, position(ls), averages(ls)) }
            .sortedWith(
                compareByDescending<TimelinePlayerEntry> { it.stats.avgGoldDiff15 != null }
                    .thenByDescending { it.stats.avgGoldDiff15 ?: 0.0 }
                    .thenByDescending { it.stats.games }
            )

    /** [order] 가 있으면 그 순서(라인 순), 없으면 경기 수 내림차순. 포지션이 비었거나 UNKNOWN 인 경기는 뺀다. */
    private fun byPosition(lines: List<Line>, order: List<String>?): List<TimelinePositionEntry> {
        val entries = lines.filter { it.line.position.uppercase() in POSITION_ORDER }
            .groupBy { it.line.position.uppercase() }
            .map { (position, ls) -> TimelinePositionEntry(position, averages(ls)) }
        return if (order != null) entries.sortedBy { order.indexOf(it.position).let { i -> if (i < 0) order.size else i } }
        else entries.sortedByDescending { it.stats.games }
    }

    private fun champions(lines: List<Line>): List<TimelineChampionEntry> =
        lines.groupBy { it.line.champion }
            .map { (champion, ls) ->
                TimelineChampionEntry(
                    champion = champion,
                    championId = ls.first().line.championId,
                    stats = averages(ls),
                    byPosition = byPosition(ls, order = null),
                )
            }
            .sortedByDescending { it.stats.games }

    private fun mainPosition(lines: List<Line>): String? =
        lines.map { it.line.position }.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

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
            avgFirstDeathMinute = ls.mapNotNull { it.firstDeathMs }
                .takeIf { it.isNotEmpty() }?.let { d -> r1(d.average() / 60_000.0) },
        )
    }

    private fun avgOrNull(values: List<Int>): Double? =
        values.takeIf { it.isNotEmpty() }?.let { r1(it.average()) }

    private fun r1(v: Double) = (v * 10).roundToInt() / 10.0

    private companion object {
        const val MODE = "all"
        const val RECENT_GAMES = 20
        /** 이만큼 뒤진 걸 뒤집어야 역전승으로 친다. 1,000 이하는 킬 하나로 오가는 폭이다. */
        const val COMEBACK_GAP = 1_500
        /** 라인 순서. UNKNOWN 은 포지션 추정이 실패한 참가자라 어느 표에도 싣지 않는다. */
        val POSITION_ORDER = Position.entries.filter { it != Position.UNKNOWN }.map { it.name }
    }
}
