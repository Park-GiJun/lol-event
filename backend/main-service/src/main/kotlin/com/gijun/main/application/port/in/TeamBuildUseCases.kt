package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.command.BuildTeamsCommand
import com.gijun.main.application.dto.result.TeamBuildResult
import com.gijun.main.application.dto.result.TeamCandidateResult

interface GetTeamCandidatesUseCase {
    /** 편성에 넣을 수 있는 사람과, 그 사람이 지금까지 간 포지션. */
    fun getTeamCandidates(): List<TeamCandidateResult>
}

interface BuildTeamsUseCase {
    /** 5 명씩 나눠 팀 평균 Elo 를 맞춘다. 묶음과 가능 포지션을 지킨다. */
    fun buildTeams(command: BuildTeamsCommand): TeamBuildResult
}
