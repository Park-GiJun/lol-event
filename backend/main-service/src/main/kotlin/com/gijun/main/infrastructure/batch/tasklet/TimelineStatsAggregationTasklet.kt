package com.gijun.main.infrastructure.batch.tasklet

import com.gijun.main.application.handler.modeToQueueIds
import com.gijun.main.application.port.out.persistence.HEATMAP_GRID
import com.gijun.main.application.port.out.persistence.HeatmapKind
import com.gijun.main.application.port.out.persistence.HeatmapPhase
import com.gijun.main.application.port.out.persistence.HeatmapScope
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.match.model.MapPoint
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.match.model.TimelineEvent
import com.gijun.main.domain.match.service.MapGeometry
import com.gijun.main.domain.match.service.PositionMetrics
import com.gijun.main.domain.match.service.TeamFightDetector
import com.gijun.main.domain.match.service.TimelineMetrics
import com.gijun.main.domain.match.service.TimelineParser
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionTimelineStatsCacheJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionTimelineStatsCacheJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PlayerTimelineStatsCacheJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PlayerTimelineStatsCacheJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PositionHeatmapCacheJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.PositionHeatmapCacheJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * 타임라인 파생 지표 집계.
 *
 * ## 왜 tasklet 하나가 테이블 셋을 쓰나
 *
 * 기존 집계 tasklet 네 개는 테이블 하나씩 맡는다. 여기서 그 패턴을 깬 이유는 **비용이 파싱에
 * 몰려 있기** 때문이다. 경기당 60KB JSON 을 트리로 올리는 게 전부이고, 사람별·챔피언별·
 * 히트맵 세 산출물이 그 한 번의 순회에서 모두 나온다. tasklet 을 셋으로 쪼개면 같은 원본을
 * 세 번 파싱한다. 독립적으로 다시 돌릴 요구도 없다.
 *
 * ## 청크
 *
 * 타임라인 원본을 읽는 첫 tasklet 이다. [TimelineParser.CHUNK_SIZE] 단위로 끊어 읽고 청크
 * 안에서 원본 문자열을 버린다 — 한 번에 들면 500경기 × 60KB 가 파싱 중인 트리와 같은 시점에
 * 살아 있다. 저장도 청크로 나눈다(히트맵 행이 수만 개가 될 수 있다).
 *
 * ## 칼바람
 *
 * `mode = "aram"` 에서는 라인 격차 컬럼이 전부 비어 나온다. 칼바람은 라인이 없어
 * [com.gijun.main.domain.rating.service.LaneScores] 의 라인 상대 목록이 빈 목록이기 때문이다.
 * 버그가 아니다.
 */
@Component
class TimelineStatsAggregationTasklet(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val playerRepo: PlayerTimelineStatsCacheJpaRepository,
    private val championRepo: ChampionTimelineStatsCacheJpaRepository,
    private val heatmapRepo: PositionHeatmapCacheJpaRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        aggregate()
        return RepeatStatus.FINISHED
    }

    @Transactional
    fun aggregate() {
        val now = LocalDateTime.now()

        for (mode in MODES) {
            val players = mutableMapOf<String, PlayerAcc>()
            val champions = mutableMapOf<ChampionKey, ChampionAcc>()
            val heatmap = mutableMapOf<HeatmapKey, Int>()
            var timelineGames = 0

            val matches = matchQueryPersistencePort.findAllWithParticipants(modeToQueueIds(mode))
            for (chunk in matches.chunked(TimelineParser.CHUNK_SIZE)) {
                val raws = matchQueryPersistencePort.findTimelineRaw(chunk.map { it.matchId })
                for (match in chunk) {
                    val raw = raws[match.matchId] ?: continue
                    if (accumulate(match, raw, players, champions, heatmap)) timelineGames++
                }
            }

            playerRepo.deleteAllByMode(mode)
            championRepo.deleteAllByMode(mode)
            heatmapRepo.deleteAllByMode(mode)

            saveAll(mode, now, players, champions, heatmap)
            log.info(
                "타임라인 집계 완료 — mode=$mode 경기 $timelineGames / ${matches.size}, " +
                    "사람 ${players.size}, 챔피언×포지션 ${champions.size}, 히트맵 셀 ${heatmap.size}",
            )
        }
    }

    // ────────── 누적 ──────────

    /** 이 경기를 실제로 셌으면 true. */
    private fun accumulate(
        match: MatchModel,
        raw: String,
        players: MutableMap<String, PlayerAcc>,
        champions: MutableMap<ChampionKey, ChampionAcc>,
        heatmap: MutableMap<HeatmapKey, Int>,
    ): Boolean {
        val timeline = TimelineParser.parse(raw)
        val metrics = TimelineMetrics.of(match, timeline) ?: return false

        val named = match.participants.filter { it.participantId > 0 && it.riotId.isNotBlank() }
        val byPid = named.associateBy { it.participantId }
        val roster = byPid.mapValues { PositionMetrics.Slot(it.value.teamId, positionOf(it.value.assignedPosition)) }
        val positions = PositionMetrics.of(timeline, roster)

        // 한타에서의 킬·데스는 교전으로 인정된 묶음만 센다.
        val fights = TeamFightDetector.of(timeline, byPid.mapValues { it.value.teamId }).filter { it.isTeamFight }
        val fightCount = mutableMapOf<Int, Int>()
        val fightKills = mutableMapOf<Int, Int>()
        val fightDeaths = mutableMapOf<Int, Int>()
        for (fight in fights) {
            fight.participantIds.forEach { fightCount.merge(it, 1, Int::plus) }
            for (kill in fight.kills) {
                if (kill.killerId > 0) fightKills.merge(kill.killerId, 1, Int::plus)
                fightDeaths.merge(kill.victimId, 1, Int::plus)
            }
        }

        for (line in metrics.players) {
            val pid = line.participantId
            val place = positions?.of(pid)
            val participant = byPid[pid] ?: continue

            players.getOrPut(line.riotId) { PlayerAcc() }.add(line, place, fightCount[pid], fightKills[pid], fightDeaths[pid])
            champions
                .getOrPut(ChampionKey(line.champion, participant.assignedPosition.ifBlank { Position.UNKNOWN.name })) {
                    ChampionAcc()
                }.add(line, place)
        }

        accumulateHeatmap(timeline, byPid, positions, heatmap)
        return true
    }

    private fun accumulateHeatmap(
        timeline: com.gijun.main.domain.match.model.MatchTimelineModel,
        byPid: Map<Int, com.gijun.main.domain.match.model.MatchParticipantModel>,
        positions: PositionMetrics.MatchPositions?,
        heatmap: MutableMap<HeatmapKey, Int>,
    ) {
        fun bump(
            scope: HeatmapScope,
            key: String,
            kind: HeatmapKind,
            at: MapPoint,
            timestampMs: Long,
        ) {
            val cell = cellOf(at) ?: return
            heatmap.merge(
                HeatmapKey(scope, key, kind, HeatmapPhase.of(timestampMs), cell.first, cell.second),
                1,
                Int::plus,
            )
        }

        for (event in timeline.events) {
            val at = event.position ?: continue
            when (event) {
                is TimelineEvent.ChampionKill -> {
                    byPid[event.victimId]?.let {
                        bump(HeatmapScope.PLAYER, it.riotId, HeatmapKind.DEATH, at, event.timestampMs)
                        bump(HeatmapScope.CHAMPION, it.champion, HeatmapKind.DEATH, at, event.timestampMs)
                    }
                    byPid[event.killerId]?.let {
                        bump(HeatmapScope.PLAYER, it.riotId, HeatmapKind.KILL, at, event.timestampMs)
                        bump(HeatmapScope.CHAMPION, it.champion, HeatmapKind.KILL, at, event.timestampMs)
                    }
                    bump(HeatmapScope.GLOBAL, "", HeatmapKind.DEATH, at, event.timestampMs)
                }
                else -> bump(HeatmapScope.GLOBAL, "", HeatmapKind.OBJECTIVE, at, event.timestampMs)
            }
        }

        // PRESENCE 는 POSITION / GLOBAL 만 채운다. 사람별·챔피언별 체류 히트맵은 분당 1점
        // 한계상 신뢰도가 가장 낮은데 저장 비용은 가장 크다.
        if (positions == null) return
        for (frame in timeline.frames) {
            if (frame.timestampMs < 60_000L) continue // 0분은 전원 분수에 있다
            for (pf in frame.participants.values) {
                val at = pf.position ?: continue
                val role = byPid[pf.participantId]?.assignedPosition?.ifBlank { Position.UNKNOWN.name } ?: continue
                bump(HeatmapScope.POSITION, role, HeatmapKind.PRESENCE, at, frame.timestampMs)
                bump(HeatmapScope.GLOBAL, "", HeatmapKind.PRESENCE, at, frame.timestampMs)
            }
        }
    }

    /** 맵 좌표를 격자 칸으로. 맵 밖이면 null. */
    private fun cellOf(at: MapPoint): Pair<Int, Int>? {
        val size = MapGeometry.MAP_MAX / HEATMAP_GRID
        val x = at.x / size
        val y = at.y / size
        val last = HEATMAP_GRID - 1
        if (x < 0 || y < 0 || x > last || y > last) return null
        return x to y
    }

    // ────────── 저장 ──────────

    private fun saveAll(
        mode: String,
        now: LocalDateTime,
        players: Map<String, PlayerAcc>,
        champions: Map<ChampionKey, ChampionAcc>,
        heatmap: Map<HeatmapKey, Int>,
    ) {
        players
            .map { (riotId, acc) -> acc.toEntity(riotId, mode, now) }
            .chunked(SAVE_CHUNK)
            .forEach(playerRepo::saveAll)

        champions
            .map { (key, acc) -> acc.toEntity(key, mode, now) }
            .chunked(SAVE_CHUNK)
            .forEach(championRepo::saveAll)

        heatmap
            .map { (key, count) ->
                PositionHeatmapCacheJpaEntity(
                    mode = mode,
                    scopeType = key.scope.name,
                    scopeKey = key.key,
                    kind = key.kind.name,
                    phase = key.phase.name,
                    gridX = key.x,
                    gridY = key.y,
                    count = count,
                    aggregatedAt = now,
                )
            }.chunked(SAVE_CHUNK)
            .forEach(heatmapRepo::saveAll)
    }

    // ────────── 누산기 ──────────

    private data class ChampionKey(
        val champion: String,
        val position: String,
    )

    private data class HeatmapKey(
        val scope: HeatmapScope,
        val key: String,
        val kind: HeatmapKind,
        val phase: HeatmapPhase,
        val x: Int,
        val y: Int,
    )

    private class PlayerAcc {
        var games = 0
        var laneGames = 0
        var framesSampled = 0
        var lanePhaseFrames = 0
        var goldDiff = 0L
        var csDiff = 0L
        var xpDiff = 0L
        var csAt10 = 0L
        var csAt10Count = 0
        var soloKills = 0L
        var firstBloods = 0
        var laneLeadGames = 0
        var laneFrames = 0L
        var laneFrameDenominator = 0
        var roamFrames = 0L
        var enemyHalfFrames = 0L
        var counterJungleFrames = 0L
        var teamfights = 0
        var teamfightKills = 0
        var teamfightDeaths = 0

        fun add(
            line: TimelineMetrics.PlayerLine,
            place: PositionMetrics.PlayerPosition?,
            fights: Int?,
            kills: Int?,
            deaths: Int?,
        ) {
            games++
            if (line.firstBlood) firstBloods++
            soloKills += line.soloKills
            line.goldDiff15?.let {
                goldDiff += it
                laneGames++
                if (it > 0) laneLeadGames++
            }
            line.csDiff15?.let { csDiff += it }
            line.xpDiff15?.let { xpDiff += it }
            line.csAt10?.let {
                csAt10 += it
                csAt10Count++
            }

            teamfights += fights ?: 0
            teamfightKills += kills ?: 0
            teamfightDeaths += deaths ?: 0

            if (place == null) return
            framesSampled += place.framesSampled
            enemyHalfFrames += frames(place.enemyHalfRate, place.framesSampled)
            counterJungleFrames += frames(place.counterJungleRate, place.framesSampled)

            // 라인 비율은 정글이 null 이다. 분모를 따로 쌓아 정글 경기가 섞여도 흐려지지 않게 한다.
            val share = place.laneShareRate ?: return
            lanePhaseFrames += place.lanePhaseFrames
            laneFrameDenominator += place.lanePhaseFrames
            laneFrames += frames(share, place.lanePhaseFrames)
            roamFrames += frames(place.roamRate ?: 0.0, place.lanePhaseFrames)
        }

        fun toEntity(
            riotId: String,
            mode: String,
            now: LocalDateTime,
        ) = PlayerTimelineStatsCacheJpaEntity(
            riotId = riotId,
            mode = mode,
            games = games,
            laneGames = laneGames,
            framesSampled = framesSampled,
            avgGoldDiff15 = avg(goldDiff, laneGames),
            avgCsDiff15 = avg(csDiff, laneGames),
            avgXpDiff15 = avg(xpDiff, laneGames),
            avgCsAt10 = avg(csAt10, csAt10Count),
            avgSoloKills = avg(soloKills, games) ?: BigDecimal.ZERO,
            firstBloodRate = rate(firstBloods.toLong(), games) ?: BigDecimal.ZERO,
            laneLeadRate = rate(laneLeadGames.toLong(), laneGames),
            laneShareRate = rate(laneFrames, laneFrameDenominator),
            roamRate = rate(roamFrames, laneFrameDenominator),
            enemyHalfRate = rate(enemyHalfFrames, framesSampled) ?: BigDecimal.ZERO,
            counterJungleRate = rate(counterJungleFrames, framesSampled) ?: BigDecimal.ZERO,
            teamfights = teamfights,
            teamfightKills = teamfightKills,
            teamfightDeaths = teamfightDeaths,
            aggregatedAt = now,
        )
    }

    private class ChampionAcc {
        var games = 0
        var wins = 0
        var laneGames = 0
        var framesSampled = 0
        var goldDiff = 0L
        var csDiff = 0L
        var xpDiff = 0L
        var csAt10 = 0L
        var csAt10Count = 0
        var soloKills = 0L
        var laneLeadGames = 0
        var laneFrames = 0L
        var lanePhaseFrames = 0
        var enemyHalfFrames = 0L

        fun add(
            line: TimelineMetrics.PlayerLine,
            place: PositionMetrics.PlayerPosition?,
        ) {
            games++
            if (line.win) wins++
            soloKills += line.soloKills
            line.goldDiff15?.let {
                goldDiff += it
                laneGames++
                if (it > 0) laneLeadGames++
            }
            line.csDiff15?.let { csDiff += it }
            line.xpDiff15?.let { xpDiff += it }
            line.csAt10?.let {
                csAt10 += it
                csAt10Count++
            }

            if (place == null) return
            framesSampled += place.framesSampled
            enemyHalfFrames += frames(place.enemyHalfRate, place.framesSampled)
            place.laneShareRate?.let {
                lanePhaseFrames += place.lanePhaseFrames
                laneFrames += frames(it, place.lanePhaseFrames)
            }
        }

        fun toEntity(
            key: ChampionKey,
            mode: String,
            now: LocalDateTime,
        ) = ChampionTimelineStatsCacheJpaEntity(
            champion = key.champion,
            mode = mode,
            position = key.position,
            games = games,
            wins = wins,
            laneGames = laneGames,
            framesSampled = framesSampled,
            sumGoldDiff15 = goldDiff,
            sumCsDiff15 = csDiff,
            sumXpDiff15 = xpDiff,
            sumCsAt10 = csAt10,
            countCsAt10 = csAt10Count,
            sumSoloKills = soloKills,
            laneLeadGames = laneLeadGames,
            sumLaneFrames = laneFrames,
            lanePhaseFrames = lanePhaseFrames,
            sumEnemyHalf = enemyHalfFrames,
            aggregatedAt = now,
        )
    }

    private fun positionOf(name: String): Position = Position.entries.firstOrNull { it.name == name.uppercase() } ?: Position.UNKNOWN

    private companion object {
        val MODES = listOf("normal", "aram", "all")

        /** 저장 청크. 히트맵 행이 수만 개가 될 수 있다. */
        const val SAVE_CHUNK = 1_000

        /**
         * 비율(0~100)과 분모로 원래 프레임 수를 되돌린다.
         *
         * [PositionMetrics] 가 비율만 돌려주므로 합계를 쌓으려면 한 번 곱해야 한다. 소수 첫째
         * 자리까지만 남은 값이라 미세한 반올림 오차가 생기지만, 여러 판을 합친 비율에서는
         * 프레임 하나 차이가 드러나지 않는다.
         */
        fun frames(
            rate: Double,
            denominator: Int,
        ): Long = Math.round(rate / 100.0 * denominator)

        fun avg(
            sum: Long,
            count: Int,
        ): BigDecimal? =
            if (count <= 0) {
                null
            } else {
                BigDecimal(sum).divide(BigDecimal(count), 1, RoundingMode.HALF_UP)
            }

        fun rate(
            part: Long,
            total: Int,
        ): BigDecimal? =
            if (total <= 0) {
                null
            } else {
                BigDecimal(part * 100).divide(BigDecimal(total), 1, RoundingMode.HALF_UP)
            }
    }
}
