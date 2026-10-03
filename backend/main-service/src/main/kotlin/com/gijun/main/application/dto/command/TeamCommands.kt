package com.gijun.main.application.dto.command

import com.gijun.main.domain.match.enums.Position

data class BuildTeamsCommand(
    val players: List<BuildTeamsPlayer>,
    /** 같은 팀이어야 하는 사람들의 묶음. */
    val togetherGroups: List<List<String>> = emptyList(),
    /** 같은 값이면 같은 편성. 바꾸면 비슷하게 좋은 다른 편성이 나온다. null 이면 0. */
    val seed: Long? = null,
    /** LLM 해설을 붙일지. 해설은 수십 초 걸린다. */
    val commentary: Boolean = true,
)

data class BuildTeamsPlayer(
    val riotId: String,
    /** 이번에 갈 수 있는 포지션. null 이거나 비면 지금까지 간 포지션으로 정한다. */
    val positions: List<Position>? = null,
)
