package com.gijun.main.infrastructure.batch.tasklet

import com.gijun.main.application.handler.query.modeToQueueIds
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.MemberPersistencePort
import com.gijun.main.infrastructure.adapter.out.persistence.batch.entity.PlayerStatsCacheEntity
import com.gijun.main.infrastructure.adapter.out.persistence.batch.repository.PlayerStatsCacheRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class PlayerStatsAggregationTasklet(
    private val matchPersistencePort: MatchPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val playerStatsCacheRepository: PlayerStatsCacheRepository,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val modes = listOf("normal", "aram", "all")
        val members = memberPersistencePort.findAll()
        val now = LocalDateTime.now()

        for (mode in modes) {
            val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

            data class Acc(
                val riotId: String,
                var wins: Int = 0, var losses: Int = 0,
                var kills: Int = 0, var deaths: Int = 0, var assists: Int = 0,
                var damage: Int = 0, var cs: Int = 0, var gold: Int = 0, var visionScore: Int = 0,
                var games: Int = 0,
                val champions: MutableMap<String, Int> = mutableMapOf(),
            )

            val accByPuuid  = members.associate { it.puuid  to Acc(it.riotId) }.toMutableMap()
            val accByRiotId = members.associate { it.riotId to accByPuuid[it.puuid]!! }

            for (match in matches) {
                for (p in match.participants) {
                    val s = (p.puuid?.let { accByPuuid[it] }) ?: accByRiotId[p.riotId] ?: continue
                    s.games++
                    if (p.win) s.wins++ else s.losses++
                    s.kills += p.kills; s.deaths += p.deaths; s.assists += p.assists
                    s.damage += p.damage; s.cs += p.cs; s.gold += p.gold; s.visionScore += p.visionScore
                    s.champions[p.champion] = (s.champions[p.champion] ?: 0) + 1
                }
            }

            fun r1(v: Double) = (v * 10).toInt() / 10.0
            fun r2(v: Double) = (v * 100).toInt() / 100.0

            // 기존 스냅샷 삭제 후 재삽입 (UPSERT)
            playerStatsCacheRepository.deleteAllByMode(mode)

            val snapshots = accByPuuid.values
                .filter { it.games > 0 }
                .map { s ->
                    val kda = if (s.deaths > 0) r2((s.kills + s.assists).toDouble() / s.deaths)
                              else (s.kills + s.assists).toDouble()
                    PlayerStatsCacheEntity(
                        riotId      = s.riotId,
                        mode        = mode,
                        games       = s.games,
                        wins        = s.wins,
                        losses      = s.losses,
                        winRate     = s.wins * 100 / s.games,
                        avgKills    = r1(s.kills.toDouble() / s.games),
                        avgDeaths   = r1(s.deaths.toDouble() / s.games),
                        avgAssists  = r1(s.assists.toDouble() / s.games),
                        kda         = kda,
                        avgDamage      = s.damage / s.games,
                        avgCs          = r1(s.cs.toDouble() / s.games),
                        avgGold        = s.gold / s.games,
                        avgVisionScore = r1(s.visionScore.toDouble() / s.games),
                        topChampion    = s.champions.maxByOrNull { it.value }?.key,
                        aggregatedAt = now,
                    )
                }

            playerStatsCacheRepository.saveAll(snapshots)
            log.info("[PlayerStats] mode=$mode → ${snapshots.size}명 집계 완료")
        }

        return RepeatStatus.FINISHED
    }
}
