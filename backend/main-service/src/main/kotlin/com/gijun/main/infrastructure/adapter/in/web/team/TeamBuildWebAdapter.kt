package com.gijun.main.infrastructure.adapter.`in`.web.team

import com.gijun.main.application.port.`in`.BuildTeamsUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.team.dto.BuildTeamsRequest
import com.gijun.main.infrastructure.adapter.`in`.web.team.dto.TeamBuildResponse
import com.gijun.main.infrastructure.adapter.`in`.web.team.dto.TeamCandidateResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 팀 편성.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/team-build/candidates` | 넣을 수 있는 사람과 각자 간 포지션 |
 * | POST | `/api/team-build` | 5 명씩 나눠 팀을 짠다 |
 * ```
 */
@RestController
@RequestMapping("/api/team-build", version = "1.0")
@Tag(name = "TeamBuild", description = "팀 편성")
class TeamBuildWebAdapter(
    private val getTeamCandidatesUseCase: GetTeamCandidatesUseCase,
    private val buildTeamsUseCase: BuildTeamsUseCase,
) {
    @GetMapping("/candidates")
    @Operation(summary = "편성 후보", description = "내전 멤버와 경기 기록이 있는 사람. 각자 간 포지션과, 화면이 처음에 켜 둘 포지션을 같이 줍니다.")
    fun getTeamCandidates(): CommonApiResponse<List<TeamCandidateResponse>> =
        CommonApiResponse.success(getTeamCandidatesUseCase.getTeamCandidates().map(TeamCandidateResponse::from))

    @PostMapping
    @Operation(
        summary = "팀 짜기",
        description =
            "인원은 10 명 이상, 5 의 배수입니다. 팀 평균 라인 Elo 가 비슷해지게 나누고, 같은 팀 묶음과 가능 포지션을 지킵니다.\n\n" +
                "편성은 코드가 계산합니다. `commentary: true` 면 LLM 해설을 붙이는데 수십 초 걸리고, " +
                "LLM 을 못 써도 편성은 그대로 돌아옵니다(`commentaryError` 에 이유).",
    )
    fun buildTeams(
        @Valid @RequestBody request: BuildTeamsRequest,
    ): CommonApiResponse<TeamBuildResponse> =
        CommonApiResponse.success(TeamBuildResponse.from(buildTeamsUseCase.buildTeams(request.toCommand())))
}
