package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 챔피언×포지션별 타임라인 지표 스냅샷.
 *
 * **평균이 아니라 합계와 개수를 담는다.** 포지션별 행을 챔피언 총합으로 롤업할 때 평균은
 * 가중치 없이 더할 수 없다. 나누는 것은 읽기 계층
 * ([com.gijun.main.infrastructure.adapter.out.persistence.statscache.StatsCacheQueryPersistenceAdapter])이 한다.
 * 기존 `champion_stats_snapshot` 이 `win_rate` 를 저장해 두는 것과 다른 선택이고,
 * 그 근거는 V20 마이그레이션 주석에 적어 뒀다.
 */
@Entity
@Table(
    name = "champion_timeline_stats_snapshot",
    schema = "lol_event",
    uniqueConstraints = [UniqueConstraint(columnNames = ["champion", "mode", "position"])],
)
class ChampionTimelineStatsCacheJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val champion: String,
    @Column(nullable = false) val mode: String,
    @Column(nullable = false) val position: String,
    @Column(nullable = false) val games: Int = 0,
    @Column(nullable = false) val wins: Int = 0,
    @Column(name = "lane_games", nullable = false) val laneGames: Int = 0,
    @Column(name = "frames_sampled", nullable = false) val framesSampled: Int = 0,
    @Column(name = "sum_gold_diff15", nullable = false) val sumGoldDiff15: Long = 0,
    @Column(name = "sum_cs_diff15", nullable = false) val sumCsDiff15: Long = 0,
    @Column(name = "sum_xp_diff15", nullable = false) val sumXpDiff15: Long = 0,
    @Column(name = "sum_cs_at10", nullable = false) val sumCsAt10: Long = 0,
    @Column(name = "count_cs_at10", nullable = false) val countCsAt10: Int = 0,
    @Column(name = "sum_solo_kills", nullable = false) val sumSoloKills: Long = 0,
    @Column(name = "lane_lead_games", nullable = false) val laneLeadGames: Int = 0,
    /** 자기 라인 회랑 안에 있던 프레임 수의 합. 분모는 [lanePhaseFrames]. */
    @Column(name = "sum_lane_frames", nullable = false) val sumLaneFrames: Long = 0,
    @Column(name = "lane_phase_frames", nullable = false) val lanePhaseFrames: Int = 0,
    /** 상대 진영에 있던 프레임 수의 합. 분모는 [framesSampled]. */
    @Column(name = "sum_enemy_half", nullable = false) val sumEnemyHalf: Long = 0,
    @Column(name = "aggregated_at", nullable = false) val aggregatedAt: LocalDateTime = LocalDateTime.now(),
)
