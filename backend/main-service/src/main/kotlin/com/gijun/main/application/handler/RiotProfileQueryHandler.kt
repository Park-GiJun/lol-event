package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.RiotMasteryResult
import com.gijun.main.application.dto.result.RiotProfileResult
import com.gijun.main.application.dto.result.RiotRankResult
import com.gijun.main.application.port.`in`.GetRiotProfileUseCase
import com.gijun.main.application.port.`in`.GetRiotProfilesUseCase
import com.gijun.main.application.port.out.external.RankedEntry
import com.gijun.main.application.port.out.external.RiotApiKtorPort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.shared.domain.vo.RiotId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Riot 공식 API 프로필 조회.
 *
 * **클래스에 `@Transactional` 을 달지 않는다.** 한 건에 Riot API 를 세 번 부르는데, 트랜잭션을
 * 걸면 그 응답을 기다리는 동안 DB 커넥션을 쥐고 있게 된다.
 */
@Service
class RiotProfileQueryHandler(
    private val riotApiKtorPort: RiotApiKtorPort,
    private val memberQueryPersistencePort: MemberQueryPersistencePort,
) : GetRiotProfileUseCase,
    GetRiotProfilesUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getRiotProfile(riotId: RiotId): RiotProfileResult {
        val puuid =
            memberQueryPersistencePort
                .findAll()
                .firstOrNull { it.riotId == riotId.value }
                ?.puuid
                ?: return RiotProfileResult.unregistered(riotId.value)

        val summoner =
            runCatching { riotApiKtorPort.getSummonerByPuuid(puuid) }
                .onFailure { log.warn("Summoner 조회 실패: ${it.message}") }
                .getOrNull()

        val rankedEntries =
            summoner
                ?.let { s ->
                    runCatching { riotApiKtorPort.getRankedEntries(s.id) }
                        .onFailure { log.warn("랭크 조회 실패: ${it.message}") }
                        .getOrNull()
                }.orEmpty()

        val mastery =
            runCatching { riotApiKtorPort.getChampionMastery(puuid, MASTERY_TOP) }
                .onFailure { log.warn("숙련도 조회 실패: ${it.message}") }
                .getOrDefault(emptyList())

        return RiotProfileResult(
            riotId = riotId.value,
            puuid = puuid,
            summonerLevel = summoner?.summonerLevel,
            profileIconId = summoner?.profileIconId,
            soloRank = rankedEntries.find { it.queueType == QUEUE_SOLO }?.let(::rankOf),
            flexRank = rankedEntries.find { it.queueType == QUEUE_FLEX }?.let(::rankOf),
            topMastery = mastery.map { RiotMasteryResult(it.championId, it.championLevel, it.championPoints) },
        )
    }

    override fun getRiotProfiles(riotIds: List<String>): Map<String, RiotProfileResult> =
        buildMap {
            for (raw in riotIds.take(BULK_LIMIT)) {
                runCatching { getRiotProfile(RiotId(raw)) }
                    .onSuccess { put(raw, it) }
                    .onFailure { log.warn("프로필 일괄 조회 실패 ($raw): ${it.message}") }
            }
        }

    private fun rankOf(entry: RankedEntry): RiotRankResult {
        val total = entry.wins + entry.losses
        return RiotRankResult(
            tier = entry.tier,
            rank = entry.rank,
            lp = entry.leaguePoints,
            wins = entry.wins,
            losses = entry.losses,
            winRate = if (total > 0) entry.wins.toDouble() / total * 100 else 0.0,
        )
    }

    private companion object {
        private const val QUEUE_SOLO = "RANKED_SOLO_5x5"
        private const val QUEUE_FLEX = "RANKED_FLEX_SR"
        private const val MASTERY_TOP = 10

        /** 한 번에 받는 인원 상한. Riot API 가 사람당 세 번 나가므로 넘기면 레이트 리밋에 걸린다. */
        private const val BULK_LIMIT = 10
    }
}
