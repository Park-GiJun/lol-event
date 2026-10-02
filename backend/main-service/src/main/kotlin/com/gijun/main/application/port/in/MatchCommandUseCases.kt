package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.command.IngestMatchCommand
import com.gijun.main.application.dto.command.SaveMatchesCommand
import com.gijun.main.application.dto.result.SaveMatchesResult
import com.gijun.main.shared.domain.vo.MatchId

interface SaveMatchesUseCase {
    /** 수집기가 올린 경기 묶음. 이미 있는 경기는 건너뛴다. */
    fun saveMatches(command: SaveMatchesCommand): SaveMatchesResult
}

interface IngestMatchUseCase {
    /**
     * 이벤트로 들어온 경기 한 판. [SaveMatchesUseCase] 와 달리 **이미 있어도 덮어쓰고**, 참가자를
     * 내전 멤버로 자동 등록한다.
     */
    fun ingestMatch(command: IngestMatchCommand)
}

interface DeleteMatchUseCase {
    fun deleteMatch(matchId: MatchId)
}
