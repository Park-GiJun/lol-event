package com.gijun.main.infrastructure.adapter.out.persistence.batch.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 좌표 히트맵 스냅샷. 원본 좌표를 쌓지 않고 **[com.gijun.main.application.port.out.HEATMAP_GRID] 격자로 접어서** 담는다.
 *
 * 500경기면 킬이 32,000건, 프레임 좌표는 200,000점이다. 화면은 어차피 격자로 뭉쳐 그리므로
 * 접어 두면 행 수가 (scope, kind, phase) 조합당 최대 1,024 로 상한이 잡힌다.
 */
@Entity
@Table(
    name = "position_heatmap_snapshot",
    schema = "lol_event",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["mode", "scope_type", "scope_key", "kind", "phase", "grid_x", "grid_y"]),
    ],
)
class PositionHeatmapCacheEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val mode: String,
    /** [com.gijun.main.application.port.out.HeatmapScope] 이름. */
    @Column(name = "scope_type", nullable = false) val scopeType: String,
    /** riotId / 챔피언명 / Position 이름 / `""`(GLOBAL). */
    @Column(name = "scope_key", nullable = false) val scopeKey: String,
    /** [com.gijun.main.application.port.out.HeatmapKind] 이름. */
    @Column(nullable = false) val kind: String,
    /** [com.gijun.main.application.port.out.HeatmapPhase] 이름. */
    @Column(nullable = false) val phase: String,
    @Column(name = "grid_x", nullable = false) val gridX: Int,
    @Column(name = "grid_y", nullable = false) val gridY: Int,
    @Column(nullable = false) val count: Int = 0,
    @Column(name = "aggregated_at", nullable = false) val aggregatedAt: LocalDateTime = LocalDateTime.now(),
)
