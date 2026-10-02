package com.gijun.main.infrastructure.adapter.out.persistence.statscache

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 사람별 타임라인 지표 스냅샷.
 *
 * `games` 는 **타임라인이 있는 경기 수**다(전체 경기가 아니다). 여기 담긴 모든 수치의
 * 모집단이라 화면에 반드시 같이 내보내야 한다.
 */
@Entity
@Table(
    name = "player_timeline_stats_snapshot",
    schema = "lol_event",
    uniqueConstraints = [UniqueConstraint(columnNames = ["riot_id", "mode"])],
)
class PlayerTimelineStatsCacheJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "riot_id", nullable = false) val riotId: String,
    @Column(nullable = false) val mode: String,
    @Column(nullable = false) val games: Int = 0,
    @Column(name = "lane_games", nullable = false) val laneGames: Int = 0,
    @Column(name = "frames_sampled", nullable = false) val framesSampled: Int = 0,
    @Column(name = "avg_gold_diff15") val avgGoldDiff15: BigDecimal? = null,
    @Column(name = "avg_cs_diff15") val avgCsDiff15: BigDecimal? = null,
    @Column(name = "avg_xp_diff15") val avgXpDiff15: BigDecimal? = null,
    @Column(name = "avg_cs_at10") val avgCsAt10: BigDecimal? = null,
    @Column(name = "avg_solo_kills", nullable = false) val avgSoloKills: BigDecimal = BigDecimal.ZERO,
    @Column(name = "first_blood_rate", nullable = false) val firstBloodRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "lane_lead_rate") val laneLeadRate: BigDecimal? = null,
    /** 정글·포지션 미상은 null. 0 을 넣으면 "라인을 안 선다"로 읽힌다. */
    @Column(name = "lane_share_rate") val laneShareRate: BigDecimal? = null,
    @Column(name = "roam_rate") val roamRate: BigDecimal? = null,
    @Column(name = "enemy_half_rate", nullable = false) val enemyHalfRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "counter_jungle_rate", nullable = false) val counterJungleRate: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false) val teamfights: Int = 0,
    @Column(name = "teamfight_kills", nullable = false) val teamfightKills: Int = 0,
    @Column(name = "teamfight_deaths", nullable = false) val teamfightDeaths: Int = 0,
    @Column(name = "aggregated_at", nullable = false) val aggregatedAt: LocalDateTime = LocalDateTime.now(),
)
