package com.gijun.main.infrastructure.batch.tasklet

import com.gijun.main.application.handler.query.modeToQueueIds
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.ChampionItemStatsCacheEntity
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.ChampionItemStatsCacheRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ChampionItemStatsAggregationTasklet(
    private val matchPersistencePort: MatchPersistencePort,
    private val championItemStatsCacheRepository: ChampionItemStatsCacheRepository,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)
    private val TRINKET_IDS = setOf(3340, 3363, 3364, 2052, 2055)

    @Transactional
    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        aggregate()
        return RepeatStatus.FINISHED
    }

    @Transactional
    fun aggregate() {
        val modes = listOf("normal", "aram", "all")
        val now   = LocalDateTime.now()

        for (mode in modes) {
            val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

            data class ItemAcc(var picks: Int = 0, var wins: Int = 0)
            val itemMap = mutableMapOf<Pair<String, Int>, ItemAcc>()

            for (match in matches) {
                for (p in match.participants) {
                    val items = listOf(p.item0, p.item1, p.item2, p.item3, p.item4, p.item5)
                    for (itemId in items) {
                        if (itemId <= 0 || itemId in TRINKET_IDS) continue
                        val key = p.champion to itemId
                        val acc = itemMap.getOrPut(key) { ItemAcc() }
                        acc.picks++
                        if (p.win) acc.wins++
                    }
                }
            }

            championItemStatsCacheRepository.deleteAllByMode(mode)

            val snapshots = itemMap.entries.map { (key, acc) ->
                val (champion, itemId) = key
                ChampionItemStatsCacheEntity(
                    champion     = champion,
                    mode         = mode,
                    itemId       = itemId,
                    picks        = acc.picks,
                    wins         = acc.wins,
                    winRate      = if (acc.picks > 0) acc.wins * 100 / acc.picks else 0,
                    aggregatedAt = now,
                )
            }

            championItemStatsCacheRepository.saveAll(snapshots)
            log.info("[ChampionItemStats] mode=$mode → ${snapshots.size}개 아이템 통계 집계 완료")
        }
    }
}
