package com.gijun.main.application.port.`in`

interface ReassignPositionsUseCase {
    /**
     * 저장된 모든 매치를 스캔해 포지션을 재배정하고 DB 를 갱신한다.
     *
     * @param force 기본값(false)은 "5포지션이 하나씩 채워져 있지 않은 깨진 팀"만 손댄다.
     *   그런데 배정 규칙 자체가 바뀌었을 때는 이걸로 아무것도 못 고친다 — 모든 팀이 이미
     *   5포지션을 하나씩 갖고 있고 값만 틀린 상태이기 때문이다. 그럴 때 true 로 전부 다시 계산한다.
     */
    fun reassignAll(force: Boolean = false): ReassignPositionsResult
}

data class ReassignPositionsResult(
    val matchesScanned: Int,
    val matchesSkippedAram: Int,
    val teamsScanned: Int,
    val teamsAlreadyValid: Int,
    val teamsFixed: Int,
    val participantsUpdated: Int,
)
