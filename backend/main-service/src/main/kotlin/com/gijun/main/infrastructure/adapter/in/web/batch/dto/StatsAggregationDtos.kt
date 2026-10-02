package com.gijun.main.infrastructure.adapter.`in`.web.batch.dto

import com.gijun.main.application.dto.result.StatsAggregationStatusResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "StatsAggregationStatusResult")
data class StatsAggregationStatusResponse(
    val playerSnapshotCount: Long,
    val championSnapshotCount: Long,
    val championItemSnapshotCount: Long,
    @field:Schema(description = "협곡 모드 플레이어 스냅샷의 가장 늦은 집계 시각. 한 번도 안 돌았으면 null.")
    val lastAggregatedAt: LocalDateTime?,
    val message: String,
) {
    companion object {
        fun from(result: StatsAggregationStatusResult) =
            StatsAggregationStatusResponse(
                playerSnapshotCount = result.playerSnapshotCount,
                championSnapshotCount = result.championSnapshotCount,
                championItemSnapshotCount = result.championItemSnapshotCount,
                lastAggregatedAt = result.lastAggregatedAt,
                message = result.message,
            )
    }
}
