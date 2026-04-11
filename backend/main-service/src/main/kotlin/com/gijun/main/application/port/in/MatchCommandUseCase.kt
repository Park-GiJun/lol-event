package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.match.command.SaveMatchesCommand
import com.gijun.main.application.dto.match.result.SaveMatchesResult

interface SaveMatchesUseCase {
    fun save(command: SaveMatchesCommand): SaveMatchesResult
}

interface DeleteMatchUseCase {
    fun delete(matchId: String)
}
