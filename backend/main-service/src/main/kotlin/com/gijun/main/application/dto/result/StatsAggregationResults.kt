package com.gijun.main.application.dto.result

import java.time.LocalDateTime

/** 스냅샷 테이블 현황. */
data class StatsAggregationStatusResult(
    val playerSnapshotCount: Long,
    val championSnapshotCount: Long,
    val championItemSnapshotCount: Long,
    /** 협곡 모드 플레이어 스냅샷의 가장 늦은 집계 시각. 한 번도 안 돌았으면 null. */
    val lastAggregatedAt: LocalDateTime?,
    val message: String,
)
