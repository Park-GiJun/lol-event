package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.MatchTimeline

/**
 * 라인 맞대결의 승자를 가리기 위한 **경기당 한 번** 계산되는 점수.
 *
 * 절댓값에는 의미가 없다. 같은 경기 안에서 같은 포지션 두 명을 비교하는 데만 쓰이므로,
 * 경기마다 단위가 달라도(15분 누적 골드 vs 분당 골드) 상관없다. 억지로 스케일을 맞추려 하지 마라.
 *
 * 사용 가능한 첫 번째 방법을 쓴다.
 */
object LaneScores {

    /** 경험치 1 = 골드 1. */
    const val W_XP = 1.0

    /**
     * 피해감소 1 = 골드 0.007. **[LaneMethod.LEGACY_FINAL] 전용이다.**
     *
     * 경기 종료 시점 누적 골드는 탱커에게 구조적으로 불리하다 (탱커 라인승률 38.7%).
     * 이 보정으로 42.0% 까지 올라왔다. 15분 기준에는 그 왜곡이 거의 없으므로
     * [LaneMethod.TIMELINE_15] 에 넣으면 오히려 이중 보정이 된다. 넣지 마라.
     */
    const val C_MIT = 0.007

    /** 라인전 구간의 끝. */
    const val LANE_PHASE_MS = 900_000L

    /**
     * 프레임 타임스탬프 드리프트 허용치.
     *
     * LCU 타임라인 프레임은 정확히 60초 간격이 아니라 조금씩 밀린다. 실측하면
     * `frames[15].timestamp = 900,330ms` 처럼 **항상 15분을 조금 넘긴다** (드리프트가 누적되고
     * 부호가 항상 양수다). `timestamp <= 900,000` 을 문자 그대로 적용하면 매 경기 15분이 아니라
     * **14분 프레임**이 잡힌다 — 방법 이름과 어긋나는 계통 오차다.
     *
     * 그래서 프레임 간격(60초)의 10% 를 허용해 "15분 프레임"을 제대로 잡는다.
     * 6초로는 여러 프레임이 동시에 걸릴 수 없으므로 경계가 모호해지지 않는다.
     */
    const val FRAME_DRIFT_TOLERANCE_MS = 6_000L

    /** 라인 개념이 없어 맞대결을 만들 수 없는 큐. */
    const val ARAM_QUEUE_ID = 3270

    /** riotId -> 라인 점수. [method] 는 이 경기에 실제로 쓰인 판정 방법이다. */
    data class Scored(val method: LaneMethod, val scores: Map<String, Double>)

    /**
     * @param timeline `game-timelines` 원본을 파싱한 것. 없으면 null 을 넘긴다.
     */
    fun of(match: Match, timeline: MatchTimeline?): Scored {
        timelineScores(match, timeline)?.let { return Scored(LaneMethod.TIMELINE_15, it) }
        return Scored(LaneMethod.LEGACY_FINAL, legacyScores(match))
    }

    /**
     * 라인 맞대결 쌍. 같은 `assignedPosition` 을 가진 사람이 **정확히 두 명이고 서로 다른 팀일 때만**
     * 성립한다. 한 팀에 같은 포지션이 둘이면 상대를 특정할 수 없으므로 그 자리는 통째로 버린다.
     *
     * 칼바람은 라인이 없다. 그런데 매치 저장 시 [PositionDetector] 가 큐를 가리지 않고 돌기 때문에
     * 칼바람 참가자에게도 포지션이 붙어 있다 — 그건 데이터가 아니라 추정기의 창작물이라
     * 여기서 명시적으로 걷어낸다. (칼바람도 팀 레이팅에는 그대로 반영된다.)
     */
    fun duels(match: Match): List<Pair<MatchParticipant, MatchParticipant>> {
        if (match.queueId == ARAM_QUEUE_ID) return emptyList()
        return match.participants
            .filter { it.riotId.isNotBlank() && it.assignedPosition.isNotBlank() }
            .groupBy { it.assignedPosition.uppercase() }
            .values
            .mapNotNull { group ->
                if (group.size != 2) return@mapNotNull null
                val (a, b) = group
                if (a.teamId == b.teamId) null else a to b
            }
    }

    // ────────── T1: 타임라인 15분 프레임 ──────────

    /** 쓸 수 없으면 null 을 돌려 다음 방법으로 넘긴다. */
    private fun timelineScores(match: Match, timeline: MatchTimeline?): Map<String, Double>? {
        if (timeline == null || timeline.isEmpty) return null

        val frame = lanePhaseFrame(timeline) ?: return null

        // participantId 는 신규 수집부터 저장한다. 과거 경기는 0 이라 프레임과 이을 수 없다.
        val byParticipantId = match.participants
            .filter { it.riotId.isNotBlank() && it.participantId > 0 }
            .associateBy { it.participantId }
        if (byParticipantId.size != match.participants.count { it.riotId.isNotBlank() }) return null

        val scores = mutableMapOf<String, Double>()
        for ((pid, p) in byParticipantId) {
            val pf = frame.participants[pid] ?: return null
            scores[p.riotId] = pf.totalGold + W_XP * pf.xp
        }
        return scores
    }

    /**
     * 15분 프레임. 없으면 마지막 프레임 (15분 전에 끝난 경기).
     *
     * 드리프트 때문에 단순 `<=` 비교가 아니라 [FRAME_DRIFT_TOLERANCE_MS] 를 얹어 고른다.
     */
    private fun lanePhaseFrame(timeline: MatchTimeline) =
        timeline.frames.lastOrNull { it.timestampMs <= LANE_PHASE_MS + FRAME_DRIFT_TOLERANCE_MS }
            ?: timeline.frames.lastOrNull()

    // ────────── T3: 경기 종료 시점 누적값 ──────────

    /**
     * 타임라인이 없는 과거 경기용. 분당으로 나누는 것은 숫자를 읽기 좋게 할 뿐,
     * 같은 경기 안의 비교에는 영향을 주지 않는다 (`gameDuration` 이 양쪽 같다).
     */
    private fun legacyScores(match: Match): Map<String, Double> {
        val minutes = (match.gameDuration / 60.0).coerceAtLeast(1.0)
        return match.participants
            .filter { it.riotId.isNotBlank() }
            .associate { p -> p.riotId to (p.gold + C_MIT * p.damageSelfMitigated) / minutes }
    }
}
