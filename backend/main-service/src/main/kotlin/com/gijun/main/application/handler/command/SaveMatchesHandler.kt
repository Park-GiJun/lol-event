package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.match.command.SaveMatchesCommand
import com.gijun.main.application.dto.match.result.SaveMatchesResult
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SaveMatchesHandler(private val matchPersistencePort: MatchPersistencePort) : SaveMatchesUseCase, DeleteMatchUseCase {

    override fun save(command: SaveMatchesCommand): SaveMatchesResult {
        var saved = 0
        var skipped = 0
        for (input in command.matches) {
            if (matchPersistencePort.existsByMatchId(input.matchId)) {
                skipped++
                continue
            }
            matchPersistencePort.save(input.toDomain())
            saved++
        }
        val total = matchPersistencePort.countByQueueIds(listOf(0, 3130, 3270))
        return SaveMatchesResult(saved, skipped, total)
    }

    override fun delete(matchId: String) = matchPersistencePort.deleteByMatchId(matchId)
}
