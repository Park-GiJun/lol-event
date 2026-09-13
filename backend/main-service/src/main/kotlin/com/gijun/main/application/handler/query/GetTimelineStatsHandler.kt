package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.GoldDiffPoint
import com.gijun.main.application.dto.stats.result.PlayerTimelineGame
import com.gijun.main.application.dto.stats.result.PlayerTimelineResult
import com.gijun.main.application.dto.stats.result.TimelinePlayerEntry
import com.gijun.main.application.dto.stats.result.TimelineStatsResult
import com.gijun.main.application.port.`in`.GetPlayerTimelineUseCase
import com.gijun.main.application.port.`in`.GetTimelineStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.service.RiotIdNormalizer
import com.gijun.main.domain.service.TimelineMetrics
import com.gijun.main.domain.service.TimelineParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToInt

/**
 * 타임라인 기반 지표. 전체 표와 개인 화면이 같은 경기 단위 계산([TimelineMetrics])을 공유한다.
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
        val decided = metrics.filter { it.goldLeadTeamAt15 != null && it.winnerTeamId != null }

        TimelineStatsResult(
            games = metrics.size,
            goldLeadWinRate = decided.takeIf { it.isNotEmpty() }
                ?.let { d -> r1(d.count { it.goldLeadTeamAt15 == it.winnerTeamId } * 100.0 / d.size) },
            goldLeadGames = decided.size,
            comebackGames = decided.count { it.goldLeadTeamAt15 != it.winnerTeamId && it.teamGoldGapAt15 >= COMEBACK_GAP },
            avgTeamGoldGapAt15 = decided.takeIf { it.isNotEmpty() }?.let { d -> r1(d.map { it.teamGoldGapAt15 }.average()) } ?: 0.0,
            players = lines(metrics)
                .groupBy { it.riotId }
                .map { (riotId, ls) -> summarize(riotId, ls) }
                .sortedWith(
                    compareByDescending<TimelinePlayerEntry> { it.avgGoldDiff15 != null }
                        .thenByDescending { it.avgGoldDiff15 ?: 0.0 }
                        .thenByDescending { it.games }
                ),
        )
    }

    override fun getPlayerTimeline(riotId: String): PlayerTimelineResult {
        val id = normalizer.canonical(riotId)
        // 개인 화면은 모드 구분이 없다 (칼바람은 라인이 없어 어차피 격차가 안 나온다).
        val mine = lines(metrics(MODE)).filter { it.riotId == id }

        val ranked = getTimelineStats(MODE).players.filter { it.avgGoldDiff15 != null }
        val rank = ranked.indexOfFirst { it.riotId == id }.takeIf { it >= 0 }?.plus(1)

        val curve = (0..TimelineMetrics.CURVE_MAX_MINUTE).mapNotNull { minute ->
            val values = mine.mapNotNull { it.goldDiffByMinute.getOrNull(minute) }
            if (values.isEmpty()) null else GoldDiffPoint(minute, r1(values.average()), values.size)
        }

        return PlayerTimelineResult(
            riotId = id,
            summary = mine.takeIf { it.isNotEmpty() }?.let { summarize(id, it) },
            goldDiffRank = rank,
            rankedPlayers = ranked.size,
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

    // ────────── 내부 ──────────

    /** 한 사람의 한 경기. riotId 는 정규화된 이름이다. */
    private class Line(
        val riotId: String,
        val matchId: String,
        val gameCreation: Long,
        val line: TimelineMetrics.PlayerLine,
    ) {
        val goldDiffByMinute get() = line.goldDiffByMinute
    }

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

    private fun summarize(riotId: String, lines: List<Line>): TimelinePlayerEntry {
        val ls = lines.map { it.line }
        val lane = ls.mapNotNull { it.goldDiff15 }
        val games = ls.size

        return TimelinePlayerEntry(
            riotId = riotId,
            games = games,
            laneGames = lane.size,
            mainPosition = ls.map { it.position }.filter { it.isNotBlank() }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
            avgGoldDiff15 = avgOrNull(lane),
            avgCsDiff15 = avgOrNull(ls.mapNotNull { it.csDiff15 }),
            avgXpDiff15 = avgOrNull(ls.mapNotNull { it.xpDiff15 }),
            laneLeadRate = lane.takeIf { it.isNotEmpty() }?.let { d -> r1(d.count { it > 0 } * 100.0 / d.size) },
            avgCsAt10 = avgOrNull(ls.mapNotNull { it.csAt10 }),
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
    }
}
