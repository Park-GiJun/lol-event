package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.IngestMatchCommand
import com.gijun.main.application.dto.command.MatchInput
import com.gijun.main.application.dto.command.SaveMatchesCommand
import com.gijun.main.application.dto.result.SaveMatchesResult
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.IngestMatchUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheCommandPort
import com.gijun.main.application.port.out.messaging.MatchEventPublishPort
import com.gijun.main.application.port.out.persistence.MatchCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.MemberCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.domain.match.service.PositionDetector
import com.gijun.main.domain.match.service.TimelineParser
import com.gijun.main.domain.member.model.MemberModel
import com.gijun.main.domain.rating.service.LaneScores
import com.gijun.main.shared.domain.vo.MatchId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 경기 저장·삭제.
 */
@Service
@Transactional
class MatchCommandHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val matchCommandPersistencePort: MatchCommandPersistencePort,
    private val memberQueryPersistencePort: MemberQueryPersistencePort,
    private val memberCommandPersistencePort: MemberCommandPersistencePort,
    private val statsResultCacheCommandPort: StatsResultCacheCommandPort,
    private val matchEventPublishPort: MatchEventPublishPort,
) : SaveMatchesUseCase,
    IngestMatchUseCase,
    DeleteMatchUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun saveMatches(command: SaveMatchesCommand): SaveMatchesResult {
        var skipped = 0
        val savedMatchIds = mutableListOf<MatchId>()
        for (input in command.matches) {
            val matchId = MatchId(input.matchId)
            if (matchQueryPersistencePort.existsByMatchId(matchId)) {
                skipped++
                continue
            }
            val match = input.toModel()
            val timeline = TimelineParser.parse(match.timelineRaw)
            val withPositions =
                match.copy(
                    participants = PositionDetector.assignPositionsToAll(match.participants).toMutableList(),
                )
            // 라인 판정 방법은 저장 시점에 한 번 확정해 둔다. 재집계가 다시 계산하더라도
            // 원본 데이터가 그대로라 같은 값이 나온다 — 여기서 남기는 건 "그때 무엇이 있었나"의 기록이다.
            val method = LaneScores.of(withPositions, timeline).method

            matchCommandPersistencePort.save(withPositions.copy(laneMethod = method))

            // 타임라인은 매치가 저장된 뒤에 넣는다 (match_timelines 가 matches 를 참조한다).
            // 없으면 그냥 건너뛴다 — 수집 실패가 매치 저장을 막으면 안 된다.
            match.timelineRaw?.takeIf { it.isNotBlank() }?.let {
                matchCommandPersistencePort.saveTimelineRaw(matchId, it)
            }

            savedMatchIds.add(matchId)
            log.debug("매치 저장 — {}, 라인 판정 {}, 타임라인 {}", matchId, method, if (match.timelineRaw != null) "있음" else "없음")
        }
        if (savedMatchIds.isNotEmpty()) {
            statsResultCacheCommandPort.evictAll()
            for (matchId in savedMatchIds) {
                publishSaved(matchId)
                log.info("매치 저장 → 레이팅 재계산 이벤트 발행: {}", matchId)
            }
        }
        val total = matchQueryPersistencePort.countByQueueIds(ALL_QUEUE_IDS)
        return SaveMatchesResult(savedMatchIds.size, skipped, total)
    }

    override fun ingestMatch(command: IngestMatchCommand) {
        val input = command.match
        // MatchInput.toModel() 은 assignedPosition 을 빈 값으로 둔다. 여기서 채우지 않으면
        // 이벤트로 들어온 매치는 관리자가 백필을 돌리기 전까지 포지션이 없는 채로 남는다.
        val match = input.toModel()
        val positioned = PositionDetector.assignPositionsToAll(match.participants)
        match.participants.clear()
        match.participants.addAll(positioned)

        // 저장은 upsert 다 — 같은 matchId 로 다시 오면 덮어쓴다.
        matchCommandPersistencePort.save(match)
        publishSaved(MatchId(input.matchId))

        autoRegisterMembers(input)
        log.info("매치 처리 완료: {} (참가자 {}명)", input.matchId, input.participants.size)
    }

    override fun deleteMatch(matchId: MatchId) {
        matchCommandPersistencePort.deleteByMatchId(matchId)
        statsResultCacheCommandPort.evictAll()
    }

    private fun publishSaved(matchId: MatchId) {
        matchEventPublishPort.publishRatingRequested(matchId)
        matchEventPublishPort.publishStatsRebuildRequested(matchId)
        matchEventPublishPort.publishRagIndexRequested(matchId)
    }

    /** 참가자 중 아직 멤버가 아닌 사람을 등록한다. PUUID 가 없는 참가자(봇 등)는 건너뛴다. */
    private fun autoRegisterMembers(input: MatchInput) {
        val riotIdByPuuid = input.participants.mapNotNull { p -> p.puuid?.let { it to p.riotId } }
        if (riotIdByPuuid.isEmpty()) return

        val existingPuuids = memberQueryPersistencePort.findAllPuuidsByPuuidIn(riotIdByPuuid.map { it.first }).toSet()
        val newcomers = riotIdByPuuid.filter { (puuid, _) -> puuid !in existingPuuids }
        if (newcomers.isEmpty()) return

        memberCommandPersistencePort.saveAll(newcomers.map { (puuid, riotId) -> MemberModel(riotId = riotId, puuid = puuid) })
        newcomers.forEach { (puuid, riotId) -> log.info("멤버 자동 등록: {} ({})", riotId, puuid) }
    }

    private companion object {
        /** 전체 경기 수는 칼바람까지 센다 — 통계용 범위([GameMode.ALL])와 다르다. */
        private val ALL_QUEUE_IDS = listOf(0, 3130, 3270)
    }
}
