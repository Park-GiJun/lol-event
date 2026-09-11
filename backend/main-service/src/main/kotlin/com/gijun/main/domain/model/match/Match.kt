package com.gijun.main.domain.model.match

import java.time.LocalDateTime

data class Match(
    val id: Long = 0,
    val matchId: String,
    val queueId: Int,
    val gameCreation: Long,
    val gameDuration: Int,
    val gameMode: String? = null,
    val gameType: String? = null,
    val gameVersion: String? = null,
    val mapId: Int? = null,
    val seasonId: Int? = null,
    val platformId: String? = null,
    /**
     * 이 경기의 라인 승자를 무엇으로 판정했는지. 저장 시점에 확정해 두고, 재집계 때 다시 계산한다
     * (원본 데이터가 그대로라 결과도 같다). 나중에 방법별 성능 비교에 쓴다.
     */
    val laneMethod: LaneMethod? = null,
    /**
     * LCU `game-timelines` 응답 **원본**. 가공하지 않는다.
     *
     * 별도 테이블(`match_timelines`)에 있어서 일반 매치 조회에는 딸려 오지 않는다.
     * 경기당 60KB 급이라 목록 화면이 통째로 끌고 오면 안 되기 때문이다.
     * 필요할 때만 `MatchPersistencePort.findTimelineRaw` 로 따로 가져온다.
     */
    val timelineRaw: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val participants: MutableList<MatchParticipant> = mutableListOf(),
    val teams: MutableList<MatchTeam> = mutableListOf()
)
