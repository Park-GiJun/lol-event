package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.StatsAggregationStatusResult
import com.gijun.main.application.port.`in`.GetStatsAggregationStatusUseCase
import com.gijun.main.application.port.out.persistence.StatsCacheQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StatsAggregationQueryHandler(
    private val statsCacheQueryPersistencePort: StatsCacheQueryPersistencePort,
) : GetStatsAggregationStatusUseCase {
    override fun getStatsAggregationStatus(): StatsAggregationStatusResult {
        val lastAt = statsCacheQueryPersistencePort.findLastPlayerAggregatedAt(GameMode.NORMAL)
        return StatsAggregationStatusResult(
            playerSnapshotCount = statsCacheQueryPersistencePort.countPlayerSnapshots(),
            championSnapshotCount = statsCacheQueryPersistencePort.countChampionSnapshots(),
            championItemSnapshotCount = statsCacheQueryPersistencePort.countChampionItemSnapshots(),
            lastAggregatedAt = lastAt,
            message = if (lastAt != null) "마지막 집계: $lastAt" else "아직 집계된 데이터가 없습니다",
        )
    }
}
