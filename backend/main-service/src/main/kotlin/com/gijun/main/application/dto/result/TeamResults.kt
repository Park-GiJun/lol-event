package com.gijun.main.application.dto.result

data class TeamCandidateResult(
    val riotId: String,
    /** 라인 레이팅 원값. 기록이 없으면 시작 점수. */
    val elo: Double,
    val games: Int,
    /** 가장 많이 간 포지션. 한 판도 안 했으면 null. */
    val mainPosition: String?,
    /** 간 적 있는 포지션과 판수. 많이 간 순. */
    val positions: List<TeamCandidatePositionResult>,
    /** 화면이 처음에 켜 둘 포지션. 충분히 가 본 자리만 든다. 기록이 없으면 다섯 자리 전부. */
    val defaultPositions: List<String>,
)

data class TeamCandidatePositionResult(
    val position: String,
    val games: Int,
)

data class TeamBuildResult(
    /** 평균 Elo 가 높은 팀부터. */
    val teams: List<TeamBuildTeamResult>,
    /** 가장 센 팀과 가장 약한 팀의 평균 Elo 차. */
    val eloSpread: Double,
    /** 가능 포지션만으로 자리를 다 채우지 못했다. `offRole` 인 사람이 있다. */
    val positionConflict: Boolean,
    /** LLM 해설. 요청하지 않았거나 못 만들었으면 null. */
    val commentary: String?,
    /** 해설을 못 만든 이유. 편성 자체는 이 값과 무관하게 유효하다. */
    val commentaryError: String?,
)

data class TeamBuildTeamResult(
    val name: String,
    val averageElo: Double,
    /** 나머지 팀들의 평균을 상대로 이길 기대 확률(0~1). 두 팀이면 상대 팀을 상대로 한 값이다. */
    val winProbability: Double,
    /** 탑 → 정글 → 미드 → 원딜 → 서포터 순. */
    val members: List<TeamBuildMemberResult>,
)

data class TeamBuildMemberResult(
    val riotId: String,
    val position: String,
    val elo: Double,
    /** 갈 수 있다고 한 포지션이 아닌 자리에 앉았다. */
    val offRole: Boolean,
)
