package com.gijun.main.domain.team.model

import com.gijun.main.domain.match.enums.Position

/** 팀 편성에 들어가는 한 사람. */
data class TeamCandidateModel(
    val riotId: String,
    /** 라인 레이팅 원값. 편성은 계산이라 표시용 수축값이 아니라 원값을 쓴다. */
    val elo: Double,
    /** 이번에 갈 수 있는 포지션. 비어 있으면 안 된다. */
    val positions: Set<Position>,
    /** 포지션별 지금까지 한 판수. 여러 자리가 되는 사람을 익숙한 자리에 먼저 앉히는 데 쓴다. */
    val positionGames: Map<Position, Int> = emptyMap(),
)

/** 팀 안의 한 자리. */
data class TeamSlotModel(
    val riotId: String,
    val position: Position,
    val elo: Double,
    /** 갈 수 있다고 한 포지션이 아닌 자리에 앉았다. 조건을 다 맞출 수 없을 때만 생긴다. */
    val offRole: Boolean,
)

data class BuiltTeamModel(
    /** 탑 → 정글 → 미드 → 원딜 → 서포터 순. */
    val slots: List<TeamSlotModel>,
    val averageElo: Double,
)

data class TeamBuildModel(
    val teams: List<BuiltTeamModel>,
    /** 가장 센 팀과 가장 약한 팀의 평균 Elo 차. */
    val eloSpread: Double,
    /** 갈 수 있는 포지션만으로는 자리를 다 채우지 못했다. `offRole` 인 사람이 있다. */
    val positionConflict: Boolean,
)
