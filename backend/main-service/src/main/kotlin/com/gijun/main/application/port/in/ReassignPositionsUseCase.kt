package com.gijun.main.application.port.`in`

interface ReassignPositionsUseCase {
    /** 저장된 모든 매치를 스캔해, 포지션이 깨진 팀만 재배정하고 DB 를 갱신한다. */
    fun reassignAll(): ReassignPositionsResult
}

data class ReassignPositionsResult(
    val matchesScanned: Int,
    val matchesSkippedAram: Int,
    val teamsScanned: Int,
    val teamsAlreadyValid: Int,
    val teamsFixed: Int,
    val participantsUpdated: Int,
)
