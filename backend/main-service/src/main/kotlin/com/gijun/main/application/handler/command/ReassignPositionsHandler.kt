package com.gijun.main.application.handler.command

import com.gijun.main.application.port.`in`.ReassignPositionsResult
import com.gijun.main.application.port.`in`.ReassignPositionsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.service.PositionDetector
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포지션 백필 — 저장된 모든 매치를 스캔해 한 팀의 5명이 TOP/JUNGLE/MID/ADC/SUPPORT 를
 * 정확히 하나씩 갖지 못한(깨진) 팀만 [PositionDetector] 로 재배정하고 assignedPosition 을 갱신한다.
 *
 * - 이미 5포지션이 정상인 팀은 건드리지 않는다 (요구사항: "정상 팀 제외").
 * - 칼바람(ARAM, queueId 3270)은 라인 개념이 없어 스킵한다.
 * - 팀원이 5명이 아닌 비정상 데이터도 스킵한다.
 *
 * 포지션 변경은 Elo 의 라인 상대 매핑·포지션별 가중치에 영향을 주므로,
 * 백필 후에는 `/api/admin/elo/reset` 으로 Elo 재집계를 권장한다.
 */
@Service
class ReassignPositionsHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val statsQueryCache: StatsCachePort,
) : ReassignPositionsUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val ARAM_QUEUE_ID = 3270
    }

    @Transactional
    override fun reassignAll(force: Boolean): ReassignPositionsResult {
        val matches = matchPersistencePort.findAllOrderedByGameCreation()
        log.info("포지션 백필 시작 — 대상 매치 ${matches.size}개")

        var matchesScanned = 0
        var matchesSkippedAram = 0
        var teamsScanned = 0
        var teamsAlreadyValid = 0
        var teamsFixed = 0

        val updates = mutableMapOf<Long, String>()

        for (match in matches) {
            if (match.queueId == ARAM_QUEUE_ID) {
                matchesSkippedAram++
                continue
            }
            matchesScanned++

            match.participants.groupBy { it.teamId }.forEach teamLoop@{ (_, team) ->
                if (team.size != 5) return@teamLoop
                teamsScanned++

                // 이미 정상 배정인 팀은 제외
                if (!force && PositionDetector.isTeamPositioned(team)) {
                    teamsAlreadyValid++
                    return@teamLoop
                }

                val positions = PositionDetector.assignPositions(team)
                var changedInTeam = false
                for (p in team) {
                    val newPos = (positions[p.riotId] ?: continue).name
                    if (p.id != 0L && p.assignedPosition != newPos) {
                        updates[p.id] = newPos
                        changedInTeam = true
                    }
                }
                if (changedInTeam) teamsFixed++
            }
        }

        if (updates.isNotEmpty()) {
            matchPersistencePort.updateAssignedPositions(updates)
            statsQueryCache.evictAll()
        }

        val result = ReassignPositionsResult(
            matchesScanned = matchesScanned,
            matchesSkippedAram = matchesSkippedAram,
            teamsScanned = teamsScanned,
            teamsAlreadyValid = teamsAlreadyValid,
            teamsFixed = teamsFixed,
            participantsUpdated = updates.size,
        )
        log.info("포지션 백필 완료 — $result")
        return result
    }
}
