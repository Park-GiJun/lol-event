package com.gijun.main.infrastructure.adapter.`in`.web.team.dto

import com.gijun.main.application.dto.result.TeamBuildMemberResult
import com.gijun.main.application.dto.result.TeamBuildResult
import com.gijun.main.application.dto.result.TeamBuildTeamResult
import com.gijun.main.application.dto.result.TeamCandidatePositionResult
import com.gijun.main.application.dto.result.TeamCandidateResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "편성 후보")
data class TeamCandidateResponse(
    val riotId: String,
    @field:Schema(description = "라인 레이팅 원값. 기록이 없으면 시작 점수")
    val elo: Double,
    val games: Int,
    val mainPosition: String?,
    @field:Schema(description = "간 적 있는 포지션과 판수. 많이 간 순")
    val positions: List<TeamCandidatePositionResponse>,
    @field:Schema(description = "화면이 처음에 켜 둘 포지션")
    val defaultPositions: List<String>,
    @field:Schema(description = "포지션 → 그 자리에 앉았을 때의 라인 Elo. 편성은 이 값으로 팀 강도를 잰다")
    val seatElo: Map<String, Double>,
) {
    companion object {
        fun from(result: TeamCandidateResult) =
            TeamCandidateResponse(
                riotId = result.riotId,
                elo = result.elo,
                games = result.games,
                mainPosition = result.mainPosition,
                positions = result.positions.map(TeamCandidatePositionResponse::from),
                defaultPositions = result.defaultPositions,
                seatElo = result.seatElo,
            )
    }
}

data class TeamCandidatePositionResponse(
    val position: String,
    val games: Int,
) {
    companion object {
        fun from(result: TeamCandidatePositionResult) = TeamCandidatePositionResponse(position = result.position, games = result.games)
    }
}

@Schema(description = "팀 편성 결과")
data class TeamBuildResponse(
    @field:Schema(description = "평균 Elo 가 높은 팀부터")
    val teams: List<TeamBuildTeamResponse>,
    @field:Schema(description = "가장 센 팀과 가장 약한 팀의 평균 Elo 차")
    val eloSpread: Double,
    @field:Schema(description = "가능 포지션만으로 자리를 다 채우지 못했다")
    val positionConflict: Boolean,
    @field:Schema(description = "LLM 해설. 요청하지 않았거나 못 만들었으면 null")
    val commentary: String?,
    @field:Schema(description = "해설을 못 만든 이유")
    val commentaryError: String?,
) {
    companion object {
        fun from(result: TeamBuildResult) =
            TeamBuildResponse(
                teams = result.teams.map(TeamBuildTeamResponse::from),
                eloSpread = result.eloSpread,
                positionConflict = result.positionConflict,
                commentary = result.commentary,
                commentaryError = result.commentaryError,
            )
    }
}

data class TeamBuildTeamResponse(
    val name: String,
    val averageElo: Double,
    @field:Schema(description = "나머지 팀들의 평균을 상대로 이길 기대 확률(0~1)")
    val winProbability: Double,
    @field:Schema(description = "탑 → 정글 → 미드 → 원딜 → 서포터 순")
    val members: List<TeamBuildMemberResponse>,
) {
    companion object {
        fun from(result: TeamBuildTeamResult) =
            TeamBuildTeamResponse(
                name = result.name,
                averageElo = result.averageElo,
                winProbability = result.winProbability,
                members = result.members.map(TeamBuildMemberResponse::from),
            )
    }
}

data class TeamBuildMemberResponse(
    val riotId: String,
    val position: String,
    val elo: Double,
    @field:Schema(description = "갈 수 있다고 한 포지션이 아닌 자리에 앉았다")
    val offRole: Boolean,
) {
    companion object {
        fun from(result: TeamBuildMemberResult) =
            TeamBuildMemberResponse(riotId = result.riotId, position = result.position, elo = result.elo, offRole = result.offRole)
    }
}
