package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.match.enums.LaneMethod
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.shared.domain.vo.MatchId

interface MatchQueryPersistencePort {
    fun existsByMatchId(matchId: MatchId): Boolean

    fun findByMatchId(matchId: MatchId): MatchModel?

    fun findAllWithParticipants(queueIds: List<Int>): List<MatchModel>

    /** 최신순 한 페이지만 참가자까지 채워서 반환한다. 목록 화면 전용. */
    fun findPageWithParticipants(
        queueIds: List<Int>,
        page: Int,
        size: Int,
    ): List<MatchModel>

    /** 기록 기간과 등장 인원. 전체 경기를 로드하지 않고 집계 쿼리로 센다. */
    fun findPeriodSummary(queueIds: List<Int>): MatchPeriodSummary

    fun countByQueueIds(queueIds: List<Int>): Long

    fun findAllOrderedByGameCreation(): List<MatchModel>

    /**
     * `[fromMs, untilMs)` 사이에 시작한 경기. 시작 시각 오름차순.
     *
     * 세션 상세처럼 하루만 보는 화면 전용이다. 전체를 로드해 메모리에서 거르면 날짜 하나짜리
     * 화면이 전체 경기를 스캔한다.
     */
    fun findInPeriodWithParticipants(
        queueIds: List<Int>,
        fromMs: Long,
        untilMs: Long,
    ): List<MatchModel>

    /** matchId -> 타임라인 원본. 없는 경기는 키 자체가 빠진다. */
    fun findTimelineRaw(matchIds: Collection<String>): Map<String, String>

    /**
     * riotId -> (포지션 -> 그 포지션으로 뛴 경기 수). 대표 포지션 계산용.
     * 전체 경기를 로드하지 않고 집계 쿼리로 센다.
     */
    fun findPositionCounts(): List<PositionCount>
}

interface MatchCommandPersistencePort {
    fun save(match: MatchModel): MatchModel

    fun deleteByMatchId(matchId: MatchId)

    /** 참가자들의 assignedPosition 만 일괄 갱신 (포지션 백필용). key = participantId, value = Position.name */
    fun updateAssignedPositions(updates: Map<Long, String>)

    /**
     * `game-timelines` 원본 저장. 같은 matchId 로 다시 부르면 덮어쓴다.
     * 타임라인 수집은 best-effort 라 실패해도 매치 저장 자체는 이미 끝나 있다.
     */
    fun saveTimelineRaw(
        matchId: MatchId,
        raw: String,
    )

    /** 재집계가 다시 판정한 laneMethod 를 일괄 반영한다. */
    fun updateLaneMethods(updates: Map<String, LaneMethod>)
}

data class PositionCount(
    val riotId: String,
    val position: String,
    val games: Long,
)

data class MatchPeriodSummary(
    val firstMatchAt: Long?,
    val lastMatchAt: Long?,
    val totalMatches: Long,
    val playerCount: Long,
)
