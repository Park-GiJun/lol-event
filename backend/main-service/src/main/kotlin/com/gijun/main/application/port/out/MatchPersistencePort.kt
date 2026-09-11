package com.gijun.main.application.port.out

import com.gijun.main.domain.model.match.LaneMethod
import com.gijun.main.domain.model.match.Match

interface MatchPersistencePort {
    fun existsByMatchId(matchId: String): Boolean
    fun findByMatchId(matchId: String): Match?
    fun save(match: Match): Match
    fun findAllWithParticipants(queueIds: List<Int>): List<Match>

    /** 최신순 한 페이지만 참가자까지 채워서 반환한다. 목록 화면 전용. */
    fun findPageWithParticipants(queueIds: List<Int>, page: Int, size: Int): List<Match>

    /** 기록 기간과 등장 인원. 전체 경기를 로드하지 않고 집계 쿼리로 센다. */
    fun findPeriodSummary(queueIds: List<Int>): MatchPeriodSummary
    fun deleteByMatchId(matchId: String)
    fun countByQueueIds(queueIds: List<Int>): Long
    fun findAllOrderedByGameCreation(): List<Match>

    /** 참가자들의 assignedPosition 만 일괄 갱신 (포지션 백필용). key = participantId, value = Position.name */
    fun updateAssignedPositions(updates: Map<Long, String>)

    /**
     * `game-timelines` 원본 저장. 같은 matchId 로 다시 부르면 덮어쓴다.
     * 타임라인 수집은 best-effort 라 실패해도 매치 저장 자체는 이미 끝나 있다.
     */
    fun saveTimelineRaw(matchId: String, raw: String)

    /** matchId -> 타임라인 원본. 없는 경기는 키 자체가 빠진다. */
    fun findTimelineRaw(matchIds: Collection<String>): Map<String, String>

    /** 재집계가 다시 판정한 laneMethod 를 일괄 반영한다. */
    fun updateLaneMethods(updates: Map<String, LaneMethod>)

    /**
     * riotId -> (포지션 -> 그 포지션으로 뛴 경기 수). 대표 포지션 계산용.
     * 전체 경기를 로드하지 않고 집계 쿼리로 센다.
     */
    fun findPositionCounts(): List<PositionCount>
}

data class PositionCount(val riotId: String, val position: String, val games: Long)

data class MatchPeriodSummary(
    val firstMatchAt: Long?,
    val lastMatchAt: Long?,
    val totalMatches: Long,
    val playerCount: Long,
)
