package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.port.out.MemberPersistencePort
import com.gijun.main.application.port.out.RiotApiPort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

data class RiotProfileResult(
    val riotId: String,
    val puuid: String?,
    val summonerLevel: Long?,
    val profileIconId: Int?,
    val soloRank: RankedInfoDto?,
    val flexRank: RankedInfoDto?,
    val topMastery: List<MasteryInfoDto>,
)

data class RankedInfoDto(
    val tier: String,
    val rank: String,
    val lp: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
)

data class MasteryInfoDto(
    val championId: Int,
    val level: Int,
    val points: Int,
)

@Tag(name = "Riot API", description = "라이엇 공식 API 연동 (랭크, 숙련도)")
@RestController
@RequestMapping("/api/riot")
class RiotApiWebAdapter(
    private val riotApiPort: RiotApiPort,
    private val memberPersistencePort: MemberPersistencePort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "플레이어 라이엇 프로필 조회 (랭크 + 숙련도)")
    @GetMapping("/profile/{riotId}")
    fun getProfile(@PathVariable riotId: String): CommonApiResponse<RiotProfileResult> {
        val member = memberPersistencePort.findAll().firstOrNull { it.riotId == riotId }
        val puuid = member?.puuid

        if (puuid == null) {
            return CommonApiResponse.success(RiotProfileResult(riotId, null, null, null, null, null, emptyList()))
        }

        val summoner = try { riotApiPort.getSummonerByPuuid(puuid) } catch (e: Exception) {
            log.warn("Summoner 조회 실패: ${e.message}")
            null
        }

        val rankedEntries = if (summoner != null) {
            try { riotApiPort.getRankedEntries(summoner.id) } catch (e: Exception) {
                log.warn("랭크 조회 실패: ${e.message}")
                emptyList()
            }
        } else emptyList()

        val mastery = try { riotApiPort.getChampionMastery(puuid, 10) } catch (e: Exception) {
            log.warn("숙련도 조회 실패: ${e.message}")
            emptyList()
        }

        val solo = rankedEntries.find { it.queueType == "RANKED_SOLO_5x5" }
        val flex = rankedEntries.find { it.queueType == "RANKED_FLEX_SR" }

        fun toRankedInfoDto(entry: com.gijun.main.application.port.out.RankedEntry?) = entry?.let {
            val total = it.wins + it.losses
            RankedInfoDto(it.tier, it.rank, it.leaguePoints, it.wins, it.losses,
                if (total > 0) (it.wins.toDouble() / total * 100) else 0.0)
        }

        return CommonApiResponse.success(RiotProfileResult(
            riotId = riotId,
            puuid = puuid,
            summonerLevel = summoner?.summonerLevel,
            profileIconId = summoner?.profileIconId,
            soloRank = toRankedInfoDto(solo),
            flexRank = toRankedInfoDto(flex),
            topMastery = mastery.map { MasteryInfoDto(it.championId, it.championLevel, it.championPoints) },
        ))
    }

    @Operation(summary = "복수 플레이어 라이엇 프로필 일괄 조회")
    @PostMapping("/profiles/bulk")
    fun getProfilesBulk(@RequestBody request: BulkProfileRequest): CommonApiResponse<Map<String, RiotProfileResult>> {
        val results = mutableMapOf<String, RiotProfileResult>()
        for (rid in request.riotIds.take(10)) {
            try {
                val profile = getProfile(rid).data
                if (profile != null) results[rid] = profile
            } catch (e: Exception) {
                log.warn("프로필 일괄 조회 실패 ($rid): ${e.message}")
            }
        }
        return CommonApiResponse.success(results)
    }
}

data class BulkProfileRequest(val riotIds: List<String> = emptyList())
