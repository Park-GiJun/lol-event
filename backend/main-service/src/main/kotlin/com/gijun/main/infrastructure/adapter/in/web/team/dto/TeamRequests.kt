package com.gijun.main.infrastructure.adapter.`in`.web.team.dto

import com.gijun.main.application.dto.command.BuildTeamsCommand
import com.gijun.main.application.dto.command.BuildTeamsPlayer
import com.gijun.main.domain.match.enums.Position
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "팀 편성 요청")
data class BuildTeamsRequest(
    @field:Size(min = 10, max = 30)
    @field:Valid
    @field:Schema(description = "편성할 사람. 10 명 이상, 5 의 배수")
    val players: List<BuildTeamsPlayerRequest> = emptyList(),
    @field:Schema(description = "같은 팀이어야 하는 사람들의 묶음. 한 묶음은 5 명까지", example = "[[\"A#KR1\",\"B#KR1\"]]")
    val togetherGroups: List<List<String>> = emptyList(),
    @field:Schema(description = "같은 값이면 같은 편성. 바꾸면 비슷하게 좋은 다른 편성이 나온다")
    val seed: Long? = null,
    @field:Schema(description = "LLM 해설을 붙일지")
    val commentary: Boolean = true,
) {
    fun toCommand() =
        BuildTeamsCommand(
            players = players.map { BuildTeamsPlayer(it.riotId, it.positions) },
            togetherGroups = togetherGroups,
            seed = seed,
            commentary = commentary,
        )
}

data class BuildTeamsPlayerRequest(
    @field:NotBlank
    @field:Schema(description = "Riot ID", example = "아랑택#아랑택")
    val riotId: String,
    @field:Schema(description = "이번에 갈 수 있는 포지션. 비우면 지금까지 간 포지션으로 정한다", example = "[\"TOP\",\"MID\"]")
    val positions: List<Position>? = null,
)
