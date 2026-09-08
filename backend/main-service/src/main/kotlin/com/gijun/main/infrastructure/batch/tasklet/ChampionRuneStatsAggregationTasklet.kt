package com.gijun.main.infrastructure.batch.tasklet

import com.gijun.main.application.handler.query.modeToQueueIds
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.ChampionRuneStatsCacheEntity
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.ChampionRuneStatsCacheRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 챔피언별 룬 통계 집계.
 *
 * 아이템 통계와 같은 틀이되 세는 단위가 다르다. 아이템은 한 경기에서 여섯 칸을 각각 세지만
 * 룬은 한 경기에 조합이 하나뿐이라 참가자 한 명이 정확히 한 표가 된다.
 *
 * 조합 키는 (핵심 룬, 주 계열, 보조 계열) 셋이다. 같은 키스톤이라도 보조 계열이 갈리면
 * 다른 빌드고 승률도 갈라지기 때문이다.
 *
 * perk0(핵심 룬)과 perkPrimaryStyle 이 0 인 참가자는 건너뛴다. 예전 수집분이나 룬 정보가
 * 실려 오지 않은 경기가 섞여 있는데, 0 을 그대로 세면 "룬 없음" 조합이 1위로 올라온다.
 */
@Component
class ChampionRuneStatsAggregationTasklet(
    private val matchPersistencePort: MatchPersistencePort,
    private val championRuneStatsCacheRepository: ChampionRuneStatsCacheRepository,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        aggregate()
        return RepeatStatus.FINISHED
    }

    @Transactional
    fun aggregate() {
        val modes = listOf("normal", "aram", "all")
        val now = LocalDateTime.now()

        for (mode in modes) {
            val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

            data class RuneAcc(var picks: Int = 0, var wins: Int = 0)
            // (champion, keystone, primaryStyle, subStyle) → 집계
            val runeMap = mutableMapOf<RuneKey, RuneAcc>()
            var skipped = 0

            for (match in matches) {
                for (p in match.participants) {
                    if (p.perk0 <= 0 || p.perkPrimaryStyle <= 0) {
                        skipped++
                        continue
                    }
                    val key = RuneKey(p.champion, p.perk0, p.perkPrimaryStyle, p.perkSubStyle)
                    val acc = runeMap.getOrPut(key) { RuneAcc() }
                    acc.picks++
                    if (p.win) acc.wins++
                }
            }

            championRuneStatsCacheRepository.deleteAllByMode(mode)

            val snapshots = runeMap.map { (key, acc) ->
                ChampionRuneStatsCacheEntity(
                    champion     = key.champion,
                    mode         = mode,
                    keystone     = key.keystone,
                    primaryStyle = key.primaryStyle,
                    subStyle     = key.subStyle,
                    picks        = acc.picks,
                    wins         = acc.wins,
                    winRate      = if (acc.picks > 0) acc.wins * 100 / acc.picks else 0,
                    aggregatedAt = now,
                )
            }

            championRuneStatsCacheRepository.saveAll(snapshots)
            log.info("[ChampionRuneStats] mode=$mode → ${snapshots.size}개 룬 조합 집계 완료 (룬 정보 없는 참가자 ${skipped}명 제외)")
        }
    }

    private data class RuneKey(
        val champion: String,
        val keystone: Int,
        val primaryStyle: Int,
        val subStyle: Int,
    )
}
