package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchTimeline
import com.gijun.main.domain.model.match.TimelineEvent
import com.gijun.main.domain.model.match.TimelineFrame
import kotlin.math.abs

/**
 * 경기 종료 스탯으로는 볼 수 없는, **시간축이 있어야만 나오는** 지표를 경기 단위로 뽑는다.
 *
 * - 라인 상대와의 10·15분 격차(골드·CS·경험치)
 * - 15분 전 킬·데스·어시스트, 솔로킬, 퍼블 관여, 첫 데스 시각
 * - 분 단위 골드 격차 곡선
 *
 * 경기 종료 스탯의 `firstBloodKill` 같은 필드와 겹치는 것도 있지만, 여기서는 이벤트에서 직접 센다.
 * 같은 소스(타임라인)에서 나온 값끼리 한 표에 놓아야 표본이 어긋나지 않는다.
 */
object TimelineMetrics {

    const val EARLY_MS = LaneScores.LANE_PHASE_MS

    /** 곡선은 30분까지만 그린다. 그 뒤로는 살아남은 경기가 적어 평균이 한두 판에 끌려다닌다. */
    const val CURVE_MAX_MINUTE = 30

    data class PlayerLine(
        val riotId: String,
        val participantId: Int,
        val teamId: Int,
        val position: String,
        val champion: String,
        val championId: Int,
        val win: Boolean,
        /** 라인 맞대결 상대. 포지션이 깨진 경기면 null 이고 격차 지표도 전부 null 이다. */
        val opponentRiotId: String?,
        val opponentChampion: String?,
        val opponentChampionId: Int?,
        val csAt10: Int?,
        val goldAt15: Int?,
        val goldDiff15: Int?,
        val csDiff15: Int?,
        val xpDiff15: Int?,
        val earlyKills: Int,
        val earlyDeaths: Int,
        val earlyAssists: Int,
        /** 도움 없이 혼자 딴 킬. 경기 전체 기준. */
        val soloKills: Int,
        /** 첫 킬의 킬러이거나 어시스트. */
        val firstBlood: Boolean,
        /** 한 번도 안 죽었으면 null. */
        val firstDeathMs: Long?,
        /** index = 분. 상대가 없으면 빈 목록. */
        val goldDiffByMinute: List<Int>,
    )

    data class MatchMetrics(
        val matchId: String,
        val gameCreation: Long,
        val players: List<PlayerLine>,
        /** 15분에 골드가 앞선 팀. 동률이거나 15분 전에 끝났으면 null. */
        val goldLeadTeamAt15: Int?,
        /** 15분 팀 골드 차이의 절댓값. */
        val teamGoldGapAt15: Int,
        val winnerTeamId: Int?,
    )

    /**
     * 타임라인과 참가자를 이을 수 없으면 null.
     * 기준은 [LaneScores] 와 같다 — 이름 있는 참가자 전원이 participantId 를 갖고 있어야 한다.
     */
    fun of(match: Match, timeline: MatchTimeline): MatchMetrics? {
        if (timeline.isEmpty) return null
        val named = match.participants.filter { it.riotId.isNotBlank() }
        if (named.isEmpty() || named.any { it.participantId <= 0 }) return null
        if (named.any { p -> timeline.frames.none { p.participantId in it.participants } }) return null

        val byPid = named.associateBy { it.participantId }
        val opponentOf = LaneScores.duels(match)
            .flatMap { (a, b) -> listOf(a.participantId to b, b.participantId to a) }
            .toMap()

        val kills = timeline.events.filterIsInstance<TimelineEvent.ChampionKill>()
        val early = kills.filter { it.timestampMs < EARLY_MS }
        val firstKill = kills.firstOrNull()
        val f10 = frameAt(timeline, 10)
        val f15 = frameAt(timeline, 15)
        val lastMinute = minOf(CURVE_MAX_MINUTE, (timeline.frames.last().timestampMs / 60_000).toInt())

        fun gold(f: TimelineFrame?, pid: Int) = f?.participants?.get(pid)?.totalGold
        fun cs(f: TimelineFrame?, pid: Int) = f?.participants?.get(pid)?.cs
        fun xp(f: TimelineFrame?, pid: Int) = f?.participants?.get(pid)?.xp
        fun diff(a: Int?, b: Int?) = if (a != null && b != null) a - b else null

        val players = named.map { p ->
            val pid = p.participantId
            val opp = opponentOf[pid]

            PlayerLine(
                riotId = p.riotId,
                participantId = pid,
                teamId = p.teamId,
                position = p.assignedPosition,
                champion = p.champion,
                championId = p.championId,
                win = p.win,
                opponentRiotId = opp?.riotId,
                opponentChampion = opp?.champion,
                opponentChampionId = opp?.championId,
                csAt10 = cs(f10, pid),
                goldAt15 = gold(f15, pid),
                goldDiff15 = opp?.let { diff(gold(f15, pid), gold(f15, it.participantId)) },
                csDiff15 = opp?.let { diff(cs(f15, pid), cs(f15, it.participantId)) },
                xpDiff15 = opp?.let { diff(xp(f15, pid), xp(f15, it.participantId)) },
                earlyKills = early.count { it.killerId == pid },
                earlyDeaths = early.count { it.victimId == pid },
                earlyAssists = early.count { pid in it.assistIds },
                soloKills = kills.count { it.killerId == pid && it.assistIds.isEmpty() },
                firstBlood = firstKill != null && (firstKill.killerId == pid || pid in firstKill.assistIds),
                firstDeathMs = kills.firstOrNull { it.victimId == pid }?.timestampMs,
                // 프레임은 1분마다 온다. 중간이 비면 거기서 끊는다 — 분 번호와 인덱스가 어긋나면 안 된다.
                goldDiffByMinute = if (opp == null) emptyList() else (0..lastMinute)
                    .map { m -> frameAt(timeline, m).let { f -> diff(gold(f, pid), gold(f, opp.participantId)) } }
                    .takeWhile { it != null }
                    .filterNotNull(),
            )
        }

        val teamGold15 = f15?.let { f ->
            named.groupBy { it.teamId }.mapValues { (_, ps) -> ps.sumOf { gold(f, it.participantId) ?: 0 } }
        }
        val leadTeam = teamGold15?.takeIf { it.size == 2 }?.let { g ->
            val (a, b) = g.entries.toList()
            when {
                a.value > b.value -> a.key
                b.value > a.value -> b.key
                else -> null
            }
        }

        return MatchMetrics(
            matchId = match.matchId,
            gameCreation = match.gameCreation,
            players = players,
            goldLeadTeamAt15 = leadTeam,
            teamGoldGapAt15 = teamGold15?.values?.let { v -> if (v.size == 2) abs(v.first() - v.last()) else 0 } ?: 0,
            winnerTeamId = named.firstOrNull { it.win }?.teamId,
        )
    }

    /**
     * [minute] 분 프레임. 경기가 그 시점까지 가지 않았으면 null.
     * 프레임 타임스탬프가 조금씩 밀리는 문제는 [LaneScores.FRAME_DRIFT_TOLERANCE_MS] 참고.
     */
    fun frameAt(timeline: MatchTimeline, minute: Int): TimelineFrame? {
        val target = minute * 60_000L
        val tol = LaneScores.FRAME_DRIFT_TOLERANCE_MS
        return timeline.frames.lastOrNull { it.timestampMs <= target + tol }
            ?.takeIf { it.timestampMs >= target - tol }
    }
}
