package com.gijun.main.infrastructure.adapter.out.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.common.exception.DomainForbiddenException
import com.gijun.common.exception.DomainNotFoundException
import com.gijun.main.application.port.out.RiotAccount
import com.gijun.main.application.port.out.RiotApiPort
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
class RiotApiAdapter(
    @Value("\${riot.api.key:}") private val apiKey: String,
    private val client: HttpClient,
    private val objectMapper: ObjectMapper
) : RiotApiPort {

    override fun getAccount(gameName: String, tagLine: String): RiotAccount = runBlocking {
        val encodedName = URLEncoder.encode(gameName, "UTF-8")
        val encodedTag = URLEncoder.encode(tagLine, "UTF-8")
        val url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/$encodedName/$encodedTag"

        val response = client.get(url) {
            header("X-Riot-Token", apiKey)
        }

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
}
