package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.match.command.SaveMatchesCommand
import com.gijun.main.application.dto.match.result.SaveMatchesResult
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.service.PositionDetector
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SaveMatchesHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val statsQueryCache: StatsCachePort,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : SaveMatchesUseCase, DeleteMatchUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(command: SaveMatchesCommand): SaveMatchesResult {
        var saved = 0
        var skipped = 0
        val savedMatchIds = mutableListOf<String>()
        for (input in command.matches) {
            if (matchPersistencePort.existsByMatchId(input.matchId)) {
                skipped++
                continue
            }
            val match = input.toDomain()
            val withPositions = match.copy(
                participants = PositionDetector.assignPositionsToAll(match.participants).toMutableList()
            )
            matchPersistencePort.save(withPositions)
            savedMatchIds.add(input.matchId)
            saved++
        }
        if (saved > 0) {
            statsQueryCache.evictAll()
            for (matchId in savedMatchIds) {
                kafkaTemplate.send("lol.elo.calculate", matchId, matchId)
                kafkaTemplate.send("lol.stats.rebuild", matchId, "match_saved")
                log.info("매치 저장 → Elo 재계산 이벤트 발행: $matchId")
            }
        }
        val total = matchPersistencePort.countByQueueIds(listOf(0, 3130, 3270))
        return SaveMatchesResult(saved, skipped, total)
    }

    override fun delete(matchId: String) {
        matchPersistencePort.deleteByMatchId(matchId)
        statsQueryCache.evictAll()
    }
}
