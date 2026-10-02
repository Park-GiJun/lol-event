package com.gijun.main.infrastructure.batch.tasklet

import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionStatsCacheJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.statscache.ChampionStatsCacheJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ChampionStatsAggregationTasklet(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val championStatsCacheRepository: ChampionStatsCacheJpaRepository,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val modes = GameMode.entries
        val now = LocalDateTime.now()

        for (mode in modes) {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)
            val allTeams = matches.flatMap { it.teams }

            // 챔피언 픽 집계
            data class ChampAcc(
                val championId: Int,
                var games: Int = 0,
                var wins: Int = 0,
            )
            val champMap = mutableMapOf<String, ChampAcc>()

            for (match in matches) {
                for (p in match.participants) {
                    val acc = champMap.getOrPut(p.champion) { ChampAcc(p.championId) }
                    acc.games++
                    if (p.win) acc.wins++
                }
            }

            // 챔피언 밴 집계
            val banMap =
                allTeams
                    .flatMap { it.bans }
                    .filter { it.championId > 0 }
                    .groupBy { it.championName }
                    .mapValues { it.value.size }

            championStatsCacheRepository.deleteAllByMode(mode.key)

            val snapshots =
                champMap.entries.map { (champion, acc) ->
                    ChampionStatsCacheJpaEntity(
                        champion = champion,
                        championId = acc.championId,
                        mode = mode.key,
                        games = acc.games,
                        wins = acc.wins,
                        winRate = if (acc.games > 0) acc.wins * 100 / acc.games else 0,
                        totalBans = banMap[champion] ?: 0,
                        aggregatedAt = now,
                    )
                }

            championStatsCacheRepository.saveAll(snapshots)
            log.info("[ChampionStats] mode=${mode.key} → ${snapshots.size}개 챔피언 집계 완료")
        }

        return RepeatStatus.FINISHED
    }
}
