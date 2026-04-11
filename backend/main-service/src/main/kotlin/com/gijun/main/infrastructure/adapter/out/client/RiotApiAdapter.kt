package com.gijun.main.infrastructure.adapter.out.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.common.exception.DomainForbiddenException
import com.gijun.common.exception.DomainNotFoundException
import com.gijun.main.application.port.out.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
class RiotApiAdapter(
    @Value("\${riot.api.key:}") private val apiKey: String,
    private val client: HttpClient,
    private val objectMapper: ObjectMapper
) : RiotApiPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val krRegion = "https://kr.api.riotgames.com"
    private val asiaRegion = "https://asia.api.riotgames.com"

    override fun getAccount(gameName: String, tagLine: String): RiotAccount = runBlocking {
        val encodedName = URLEncoder.encode(gameName, "UTF-8")
        val encodedTag = URLEncoder.encode(tagLine, "UTF-8")
        val url = "$asiaRegion/riot/account/v1/accounts/by-riot-id/$encodedName/$encodedTag"

        val response = client.get(url) { header("X-Riot-Token", apiKey) }

        when (response.status) {
            HttpStatusCode.NotFound -> throw DomainNotFoundException("라이엇 계정을 찾을 수 없습니다: $gameName#$tagLine")
            HttpStatusCode.Forbidden -> throw DomainForbiddenException("Riot API 키가 만료됐습니다")
        }

        @Suppress("UNCHECKED_CAST")
        val map = objectMapper.readValue(response.bodyAsText(), Map::class.java) as Map<String, Any>
        RiotAccount(
            puuid = map["puuid"] as String,
            gameName = map["gameName"] as String,
            tagLine = map["tagLine"] as String
        )
    }

    override fun getSummonerByPuuid(puuid: String): SummonerData? = runBlocking {
        try {
            val url = "$krRegion/lol/summoner/v4/summoners/by-puuid/$puuid"
            val response = client.get(url) { header("X-Riot-Token", apiKey) }
            if (!response.status.isSuccess()) return@runBlocking null

            @Suppress("UNCHECKED_CAST")
            val map = objectMapper.readValue(response.bodyAsText(), Map::class.java) as Map<String, Any>
            SummonerData(
                id = map["id"] as String,
                accountId = map["accountId"] as String,
                puuid = map["puuid"] as String,
                profileIconId = (map["profileIconId"] as Number).toInt(),
                summonerLevel = (map["summonerLevel"] as Number).toLong(),
            )
        } catch (e: Exception) {
            log.warn("Summoner 조회 실패 (puuid=$puuid): ${e.message}")
            null
        }
    }

    override fun getRankedEntries(summonerId: String): List<RankedEntry> = runBlocking {
        try {
            val url = "$krRegion/lol/league/v4/entries/by-summoner/$summonerId"
            val response = client.get(url) { header("X-Riot-Token", apiKey) }
            if (!response.status.isSuccess()) return@runBlocking emptyList()

            @Suppress("UNCHECKED_CAST")
            val list = objectMapper.readValue(response.bodyAsText(), List::class.java) as List<Map<String, Any>>
            list.map { m ->
                RankedEntry(
                    queueType = m["queueType"] as? String ?: "",
                    tier = m["tier"] as? String ?: "",
                    rank = m["rank"] as? String ?: "",
                    leaguePoints = (m["leaguePoints"] as? Number)?.toInt() ?: 0,
                    wins = (m["wins"] as? Number)?.toInt() ?: 0,
                    losses = (m["losses"] as? Number)?.toInt() ?: 0,
                )
            }
        } catch (e: Exception) {
            log.warn("랭크 조회 실패 (summonerId=$summonerId): ${e.message}")
            emptyList()
        }
    }

    override fun getChampionMastery(puuid: String, top: Int): List<ChampionMasteryData> = runBlocking {
        try {
            val url = "$krRegion/lol/champion-mastery/v4/champion-masteries/by-puuid/$puuid/top?count=$top"
            val response = client.get(url) { header("X-Riot-Token", apiKey) }
            if (!response.status.isSuccess()) return@runBlocking emptyList()

            @Suppress("UNCHECKED_CAST")
            val list = objectMapper.readValue(response.bodyAsText(), List::class.java) as List<Map<String, Any>>
            list.map { m ->
                ChampionMasteryData(
                    championId = (m["championId"] as? Number)?.toInt() ?: 0,
                    championLevel = (m["championLevel"] as? Number)?.toInt() ?: 0,
                    championPoints = (m["championPoints"] as? Number)?.toInt() ?: 0,
                )
            }
        } catch (e: Exception) {
            log.warn("숙련도 조회 실패 (puuid=$puuid): ${e.message}")
            emptyList()
        }
    }
}
