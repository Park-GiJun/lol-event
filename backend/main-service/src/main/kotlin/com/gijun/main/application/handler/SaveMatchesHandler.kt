package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.SaveMatchesCommand
import com.gijun.main.application.dto.result.SaveMatchesResult
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheCommandPort
import com.gijun.main.application.port.out.persistence.MatchCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.service.PositionDetector
import com.gijun.main.domain.match.service.TimelineParser
import com.gijun.main.domain.rating.service.LaneScores
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SaveMatchesHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val matchCommandPersistencePort: MatchCommandPersistencePort,
    private val statsResultCacheCommandPort: StatsResultCacheCommandPort,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : SaveMatchesUseCase,
    DeleteMatchUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(command: SaveMatchesCommand): SaveMatchesResult {
        var saved = 0
        var skipped = 0
        val savedMatchIds = mutableListOf<String>()
        for (input in command.matches) {
            if (matchQueryPersistencePort.existsByMatchId(input.matchId)) {
                skipped++
                continue
            }
            val match = input.toModel()
            val timeline = TimelineParser.parse(match.timelineRaw)
            val withPositions =
                match.copy(
                    participants = PositionDetector.assignPositionsToAll(match.participants).toMutableList(),
                )
            // 라인 판정 방법은 저장 시점에 한 번 확정해 둔다. 재집계가 다시 계산하더라도
            // 원본 데이터가 그대로라 같은 값이 나온다 — 여기서 남기는 건 "그때 무엇이 있었나"의 기록이다.
            val method = LaneScores.of(withPositions, timeline).method

            matchCommandPersistencePort.save(withPositions.copy(laneMethod = method))

            // 타임라인은 매치가 저장된 뒤에 넣는다 (match_timelines 가 matches 를 참조한다).
            // 없으면 그냥 건너뛴다 — 수집 실패가 매치 저장을 막으면 안 된다.
            match.timelineRaw?.takeIf { it.isNotBlank() }?.let {
                matchCommandPersistencePort.saveTimelineRaw(input.matchId, it)
            }

            savedMatchIds.add(input.matchId)
            saved++
            log.debug("매치 저장 — ${input.matchId}, 라인 판정 $method, 타임라인 ${if (match.timelineRaw != null) "있음" else "없음"}")
        }
        if (saved > 0) {
            statsResultCacheCommandPort.evictAll()
            for (matchId in savedMatchIds) {
                kafkaTemplate.send("lol.elo.calculate", matchId, matchId)
                kafkaTemplate.send("lol.stats.rebuild", matchId, "match_saved")
                log.info("매치 저장 → 레이팅 재계산 이벤트 발행: $matchId")
            }
        }
        val total = matchQueryPersistencePort.countByQueueIds(listOf(0, 3130, 3270))
        return SaveMatchesResult(saved, skipped, total)
    }

    override fun delete(matchId: String) {
        matchCommandPersistencePort.deleteByMatchId(matchId)
        statsResultCacheCommandPort.evictAll()
    }
}
